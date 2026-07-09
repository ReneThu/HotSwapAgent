package micronaut;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Owns the lifecycle of the currently running demo process.
 *
 * <p>Centralising the state here keeps start/stop idempotent and thread-safe, and the
 * {@link PreDestroy} hook guarantees the demo (and every JVM it spawned) is torn down when the
 * backend itself shuts down — so no orphaned processes or bound ports survive a demo run.
 */
@Singleton
public class DemoProcessManager {

    private final DemoLauncher launcher;
    private CodeExecutionElement current;

    public DemoProcessManager(DemoLauncher launcher) {
        this.launcher = launcher;
    }

    public synchronized boolean start() {
        if (current != null && current.isRunning()) {
            return false;
        }
        current = launcher.newExecution();
        current.run();
        return true;
    }

    public synchronized void stop() {
        if (current != null) {
            current.stopTask();
            current = null;
        }
    }

    public synchronized boolean isRunning() {
        return current != null && current.isRunning();
    }

    public synchronized String output() {
        if (current == null) {
            return "";
        }
        List<String> lines = current.getRingBufferContents();
        return String.join("\n", lines);
    }

    @PreDestroy
    public void onShutdown() {
        stop();
    }
}
