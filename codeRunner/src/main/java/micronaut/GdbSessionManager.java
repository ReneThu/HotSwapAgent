package micronaut;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import java.util.function.Consumer;

/**
 * Owns the lifecycle of the single active gdb debugging session.
 *
 * <p>Only one slowdebug JVM is debugged at a time, so starting a new session first tears down any
 * previous one. Centralising the state here keeps start/stop idempotent and thread-safe, and the
 * {@link PreDestroy} hook guarantees gdb and the debuggee JVM are killed when the backend shuts
 * down — no orphaned processes survive a demo.
 */
@Singleton
public class GdbSessionManager {

    private final GdbLauncher launcher;
    private GdbPtySession current;

    public GdbSessionManager(GdbLauncher launcher) {
        this.launcher = launcher;
    }

    public synchronized void start(Consumer<byte[]> outputConsumer) {
        stopInternal();
        current = launcher.newSession();
        current.start(outputConsumer);
    }

    public synchronized void write(String data) {
        if (current != null) {
            current.write(data);
        }
    }

    public synchronized void stop() {
        stopInternal();
    }

    public synchronized boolean isRunning() {
        return current != null && current.isRunning();
    }

    private void stopInternal() {
        if (current != null) {
            current.stop();
            current = null;
        }
    }

    @PreDestroy
    public void onShutdown() {
        stop();
    }
}
