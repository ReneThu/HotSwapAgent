package micronaut;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owns the lifecycle of the single active gdb debugging session and multiplexes it to every
 * connected browser terminal.
 *
 * <p>Only one slowdebug JVM is debugged at a time, but several browser windows may watch it at once
 * — in Slidev presenter mode the presenter window and the projected audience window each open their
 * own terminal. Both attach to the <em>same</em> gdb session here, so a command typed (or pasted) in
 * one window is echoed by the shared PTY and broadcast to every window. New arrivals are replayed
 * the output history first, so a window that joins late (e.g. the audience view opening the slide a
 * moment after the presenter) sees the identical screen.
 *
 * <p>The session is created on the first attach and torn down when the last client detaches, so gdb
 * and the debuggee JVM never outlive the slide. The {@link PreDestroy} hook guarantees the same on
 * backend shutdown — no orphaned slowdebug JVMs survive a demo.
 */
@Singleton
public class GdbSessionManager {

    /** Cap on the replayable output history (bytes). Generous for a demo; bounds memory. */
    private static final int MAX_HISTORY_BYTES = 8 * 1024 * 1024;

    private final GdbLauncher launcher;

    private final List<Consumer<byte[]>> consumers = new ArrayList<>();
    private final Deque<byte[]> history = new ArrayDeque<>();
    private int historyBytes = 0;

    private GdbPtySession current;

    public GdbSessionManager(GdbLauncher launcher) {
        this.launcher = launcher;
    }

    /**
     * Attach a browser terminal to the single shared gdb session, creating the session on the first
     * attach. The current output history is replayed to the newcomer first so its screen matches the
     * terminals already connected, then it starts receiving live output.
     */
    public synchronized void attach(Consumer<byte[]> consumer) {
        if (current == null || !current.isRunning()) {
            resetHistory();
            consumers.clear();
            current = launcher.newSession();
            current.start(this::onOutput);
        }
        byte[] backlog = snapshotHistory();
        if (backlog.length > 0) {
            consumer.accept(backlog);
        }
        consumers.add(consumer);
    }

    /** Detach a browser terminal. When the last client leaves, the shared gdb session is torn down. */
    public synchronized void detach(Consumer<byte[]> consumer) {
        consumers.remove(consumer);
        if (consumers.isEmpty()) {
            stopInternal();
        }
    }

    public synchronized void write(String data) {
        if (current != null) {
            current.write(data);
        }
    }

    public synchronized boolean isRunning() {
        return current != null && current.isRunning();
    }

    public synchronized void stop() {
        consumers.clear();
        stopInternal();
    }

    /**
     * Fan out one chunk of PTY output to every attached terminal and append it to the replay
     * history. Synchronised on the same monitor as {@link #attach}, so a client is either included in
     * the history snapshot it replays or in this broadcast — never both and never neither.
     */
    private synchronized void onOutput(byte[] bytes) {
        appendHistory(bytes);
        for (Consumer<byte[]> consumer : consumers) {
            try {
                consumer.accept(bytes);
            } catch (RuntimeException ignored) {
                // A single closed/misbehaving client must not break the broadcast to the others.
            }
        }
    }

    private void appendHistory(byte[] bytes) {
        history.addLast(bytes);
        historyBytes += bytes.length;
        while (historyBytes > MAX_HISTORY_BYTES && history.size() > 1) {
            historyBytes -= history.removeFirst().length;
        }
    }

    private byte[] snapshotHistory() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(historyBytes, 32));
        for (byte[] chunk : history) {
            buffer.write(chunk, 0, chunk.length);
        }
        return buffer.toByteArray();
    }

    private void resetHistory() {
        history.clear();
        historyBytes = 0;
    }

    private void stopInternal() {
        if (current != null) {
            current.stop();
            current = null;
        }
        resetHistory();
    }

    @PreDestroy
    public void onShutdown() {
        stop();
    }
}
