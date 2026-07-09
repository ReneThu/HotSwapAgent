package micronaut;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeExecutionElementTest {

    @Test
    @Timeout(30)
    void capturesOutputIntoRingBuffer() throws Exception {
        CodeExecutionElement element = new CodeExecutionElement(
                List.of("bash", "-c", "echo hello; echo world"));
        element.run();

        List<String> output = waitForOutputContaining(element, "world");
        assertTrue(output.contains("hello"));
        assertTrue(output.contains("world"));

        element.stopTask();
    }

    @Test
    @Timeout(30)
    void ringBufferKeepsOnlyMostRecentLines() throws Exception {
        CodeExecutionElement element = new CodeExecutionElement(
                List.of("bash", "-c", "for i in $(seq 1 25); do echo line$i; done"));
        element.run();

        List<String> output = waitForOutputContaining(element, "line25");
        assertEquals(10, output.size(), "ring buffer should be capped at 10 lines");
        assertTrue(output.contains("line25"));
        assertTrue(output.contains("line16"));
        assertFalse(output.contains("line15"), "older lines should have been evicted");

        element.stopTask();
    }

    @Test
    @Timeout(30)
    void stopTaskKillsProcessAndDescendants() throws Exception {
        // The parent shell spawns a long-lived child and prints its PID so the test can track it.
        CodeExecutionElement element = new CodeExecutionElement(
                List.of("bash", "-c", "sleep 300 & echo $!; wait"));
        element.run();

        List<String> output = waitForOutputNonEmpty(element);
        long childPid = Long.parseLong(output.get(0).trim());
        assertTrue(isAlive(childPid), "child process should be running before stop");

        element.stopTask();

        assertFalse(element.isRunning(), "process should be stopped");
        assertTrue(waitUntil(() -> !isAlive(childPid), 5000),
                "descendant process should be killed by stopTask");
    }

    @Test
    void stopTaskBeforeRunIsSafe() {
        CodeExecutionElement element = new CodeExecutionElement(List.of("true"));
        assertDoesNotThrow(element::stopTask);
        assertFalse(element.isRunning());
    }

    private static boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private List<String> waitForOutputContaining(CodeExecutionElement element, String needle)
            throws InterruptedException {
        waitUntil(() -> element.getRingBufferContents().stream().anyMatch(line -> line.contains(needle)), 10_000);
        return element.getRingBufferContents();
    }

    private List<String> waitForOutputNonEmpty(CodeExecutionElement element) throws InterruptedException {
        waitUntil(() -> !element.getRingBufferContents().isEmpty(), 10_000);
        return element.getRingBufferContents();
    }

    private boolean waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.getAsBoolean();
    }
}
