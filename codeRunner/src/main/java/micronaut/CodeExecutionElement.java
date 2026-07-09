package micronaut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Runs an external command (the demo JVM launched through Gradle) and keeps a small ring buffer
 * of its most recent output. Stopping the task tears down the whole process tree so no orphaned
 * JVMs or bound ports are left behind between demo runs.
 */
public class CodeExecutionElement {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final List<String> command;
    private final Map<String, String> environment;
    private Process process;
    private BufferedReader reader;
    private Thread outputThread;

    private final LinkedList<String> ringBuffer = new LinkedList<>();
    private final int bufferSize = 10;
    private final Lock lock = new ReentrantLock();

    public CodeExecutionElement(List<String> command) {
        this(command, Collections.emptyMap());
    }

    public CodeExecutionElement(List<String> command, Map<String, String> environment) {
        this.command = List.copyOf(command);
        this.environment = Map.copyOf(environment);
    }

    public void run() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().putAll(environment);
            processBuilder.redirectErrorStream(true);

            this.process = processBuilder.start();

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            this.outputThread = new Thread(this::readOutput, "code-runner-output");
            this.outputThread.setDaemon(true);
            this.outputThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readOutput() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lock.lock();
                try {
                    if (ringBuffer.size() == bufferSize) {
                        ringBuffer.removeFirst();
                    }
                    ringBuffer.addLast(line);
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException e) {
            // The stream is closed when the process is stopped; this is expected on teardown.
        }
    }

    public List<String> getRingBufferContents() {
        lock.lock();
        try {
            return new ArrayList<>(ringBuffer);
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public void stopTask() {
        if (process != null) {
            // Capture descendants before destroying the parent: once the parent exits its
            // children are reparented and would no longer be reachable via descendants().
            List<ProcessHandle> descendants = process.descendants().collect(Collectors.toList());

            process.destroy();
            descendants.forEach(ProcessHandle::destroy);

            if (!awaitTermination(process, descendants)) {
                process.destroyForcibly();
                descendants.forEach(ProcessHandle::destroyForcibly);
                awaitTermination(process, descendants);
            }
        }

        if (outputThread != null) {
            outputThread.interrupt();
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // Nothing useful to do if the already-terminated stream fails to close.
            }
        }

        process = null;
        reader = null;
        outputThread = null;
    }

    private boolean awaitTermination(Process process, List<ProcessHandle> descendants) {
        try {
            boolean parentDone = process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (ProcessHandle descendant : descendants) {
                descendant.onExit().get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            return parentDone && descendants.stream().noneMatch(ProcessHandle::isAlive);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return descendants.stream().noneMatch(ProcessHandle::isAlive) && !process.isAlive();
        }
    }
}
