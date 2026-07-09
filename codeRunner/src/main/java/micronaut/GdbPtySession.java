package micronaut;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Owns one PTY-wrapped {@code gdb} process and bridges it to a consumer.
 *
 * <p>Raw bytes read from the PTY (gdb's prompt, colours, escape sequences) are forwarded verbatim to
 * the supplied consumer so a browser xterm.js terminal renders them exactly as a native terminal
 * would. Keystrokes flow the other way through {@link #write(String)}.
 *
 * <p>{@link #stop()} tears down the whole tree ({@code script} → gdb → the debuggee JVM) using the
 * same descendant-capture strategy as {@link CodeExecutionElement}, so no orphaned slowdebug JVMs
 * survive a demo run.
 */
public class GdbPtySession {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final List<String> command;
    private Process process;
    private Thread readerThread;
    private volatile Consumer<byte[]> outputConsumer;

    public GdbPtySession(List<String> command) {
        this.command = List.copyOf(command);
    }

    public synchronized void start(Consumer<byte[]> outputConsumer) {
        this.outputConsumer = outputConsumer;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            this.process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to launch gdb session", e);
        }
        this.readerThread = new Thread(this::pumpOutput, "gdb-pty-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    private void pumpOutput() {
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                Consumer<byte[]> consumer = outputConsumer;
                if (consumer != null && read > 0) {
                    consumer.accept(Arrays.copyOf(buffer, read));
                }
            }
        } catch (IOException e) {
            // The PTY is closed when the session is stopped; expected on teardown.
        }
    }

    public synchronized void write(String data) {
        if (process == null || !process.isAlive()) {
            return;
        }
        try {
            OutputStream out = process.getOutputStream();
            out.write(data.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            // The PTY is gone; nothing useful to do with keystrokes for a dead session.
        }
    }

    public synchronized boolean isRunning() {
        return process != null && process.isAlive();
    }

    public synchronized void stop() {
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

        if (readerThread != null) {
            readerThread.interrupt();
        }

        process = null;
        readerThread = null;
        outputConsumer = null;
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
