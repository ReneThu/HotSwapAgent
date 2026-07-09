package micronaut;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Builds an interactive {@code gdb} session, wrapped in a pseudo-terminal, that debugs the locally
 * built <em>slowdebug</em> JVM while it runs the {@code memoryLeakSample} metaspace leak.
 *
 * <p>A real TTY is required so gdb behaves like a native terminal (prompt, readline, colours). Java
 * has no built-in PTY, so the command is launched through {@code script -q -f -c <wrapper> /dev/null}
 * (util-linux), which allocates the PTY. To sidestep shell-quoting problems the actual launch is a
 * generated wrapper script that fixes the terminal geometry and {@code exec}s gdb with a generated
 * init file. The init file passes SIGSEGV/SIGBUS/SIGFPE straight through — the JVM uses those
 * signals internally — so the debugger never stops on them.
 *
 * <p>Breakpoints are intentionally <strong>not</strong> pre-set: the presenter places them live from
 * the terminal (see {@code GDB-CHEATSHEET.md}) to showcase the workflow.
 */
@Singleton
public class GdbLauncher {

    private final String gdbBinary;
    private final String ptyWrapper;
    private final String javaBinary;
    private final String agentJar;
    private final String swapsDir;
    private final String initialClasspath;
    private final String mainClass;
    private final long durationMs;
    private final int numVersions;
    private final int printEvery;
    private final int targetMetaspaceMb;
    private final int termCols;
    private final int termRows;

    @Inject
    public GdbLauncher(
            @Value("${gdb.gdb-binary}") String gdbBinary,
            @Value("${gdb.pty-wrapper}") String ptyWrapper,
            @Value("${gdb.java-binary}") String javaBinary,
            @Value("${gdb.agent-jar}") String agentJar,
            @Value("${gdb.swaps-dir}") String swapsDir,
            @Value("${gdb.initial-classpath}") String initialClasspath,
            @Value("${gdb.main-class}") String mainClass,
            @Value("${gdb.duration-ms}") long durationMs,
            @Value("${gdb.num-versions}") int numVersions,
            @Value("${gdb.print-every}") int printEvery,
            @Value("${gdb.target-metaspace-mb}") int targetMetaspaceMb,
            @Value("${gdb.term-cols}") int termCols,
            @Value("${gdb.term-rows}") int termRows) {
        this.gdbBinary = gdbBinary;
        this.ptyWrapper = ptyWrapper;
        this.javaBinary = javaBinary;
        this.agentJar = agentJar;
        this.swapsDir = swapsDir;
        this.initialClasspath = initialClasspath;
        this.mainClass = mainClass;
        this.durationMs = durationMs;
        this.numVersions = numVersions;
        this.printEvery = printEvery;
        this.targetMetaspaceMb = targetMetaspaceMb;
        this.termCols = termCols;
        this.termRows = termRows;
    }

    public GdbPtySession newSession() {
        Path gdbInit = writeTempFile("gdb-init", ".gdb", gdbInitContents(), false);
        Path wrapper = writeTempFile("gdb-wrapper", ".sh", wrapperContents(gdbInit), true);
        List<String> command = List.of(ptyWrapper, "-q", "-f", "-c", wrapper.toString(), "/dev/null");
        return new GdbPtySession(command);
    }

    private String gdbInitContents() {
        return String.join("\n",
                "set pagination off",
                "set confirm off",
                "set breakpoint pending on",
                "set overload-resolution off",
                "handle SIGSEGV nostop noprint pass",
                "handle SIGBUS nostop noprint pass",
                "handle SIGFPE nostop noprint pass",
                "");
    }

    private String wrapperContents(Path gdbInit) {
        String execLine = "exec \"" + gdbBinary + "\" -nx -x \"" + gdbInit + "\" --args \"" + javaBinary + "\""
                + " -javaagent:\"" + agentJar + "\""
                + " -cp \"" + initialClasspath + "\""
                + " " + mainClass
                + " " + durationMs
                + " \"" + swapsDir + "\""
                + " " + numVersions
                + " " + printEvery
                + " " + targetMetaspaceMb;

        // A readable representation of the command shown to the presenter (real invocation uses
        // full paths). Kept short so it reads cleanly on the slide.
        String displayCmd = "gdb -x gdb-init.gdb --args " + baseName(javaBinary)
                + " -javaagent:" + baseName(agentJar)
                + " -cp " + shortClasspath(initialClasspath)
                + " " + mainClass;

        return String.join("\n",
                "#!/usr/bin/env bash",
                "stty rows " + termRows + " cols " + termCols + " 2>/dev/null",
                "export TERM=xterm-256color",
                "clear",
                // Show the gdb start command and wait — pressing Enter launches the debug session.
                "printf '\\033[2m# Live JVM debugging \\342\\200\\224 press Enter to launch gdb on the slowdebug build\\033[0m\\n'",
                "printf '\\033[1;32mpresenter@jvm-debug\\033[0m:\\033[1;34m~/jdk\\033[0m$ %s' '" + displayCmd + "'",
                "read -r _",
                "echo",
                execLine,
                "");
    }

    private static String baseName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    // Render a classpath entry as its last two path segments (e.g. .../memoryLeakSample/swaps/00000
    // -> swaps/00000) so the displayed command stays short but recognisable.
    private static String shortClasspath(String path) {
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int last = trimmed.lastIndexOf('/');
        if (last < 0) {
            return trimmed;
        }
        int prev = trimmed.lastIndexOf('/', last - 1);
        return prev < 0 ? trimmed.substring(last + 1) : trimmed.substring(prev + 1);
    }

    private Path writeTempFile(String prefix, String suffix, String contents, boolean executable) {
        try {
            Path path = Files.createTempFile(prefix, suffix);
            Files.writeString(path, contents);
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            if (executable) {
                perms.add(PosixFilePermission.OWNER_EXECUTE);
            }
            Files.setPosixFilePermissions(path, perms);
            path.toFile().deleteOnExit();
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write gdb launch file", e);
        }
    }
}
