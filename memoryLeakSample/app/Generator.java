import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Ahead-of-time generator for the pre-compiled swap classes.
 *
 * <p>Every version of {@code demo.Leaker} is byte-for-byte identical except for a fixed-width label
 * string ("Metaspace leak - swap #NNNNN / TTTTT"). We therefore compile the template exactly once
 * and then produce every version by cloning the compiled bytes and overwriting the 5 index digits in
 * place. The label length never changes, so the constant-pool entry stays valid and every output is
 * a legal redefinition of every other - and generation takes milliseconds instead of thousands of
 * javac invocations.
 *
 * <p>Output layout: {@code <outDir>/NNNNN/demo/Leaker.class}. Usage:
 * {@code java Generator <template> <outDir> <numVersions>}
 */
public final class Generator {

    private static final String LABEL_PREFIX = "Metaspace leak - swap #";

    public static void main(String[] args) throws Exception {
        Path template = Path.of(args[0]);
        Path outDir = Path.of(args[1]);
        int numVersions = Integer.parseInt(args[2]);
        if (numVersions < 1 || numVersions > 99_999) {
            throw new IllegalArgumentException("numVersions must be in [1, 99999], got " + numVersions);
        }

        String total = String.format("%05d", numVersions);
        String label = LABEL_PREFIX + "00000 / " + total;
        String source = Files.readString(template).replace("__SWAP_LABEL__", label);

        byte[] base = compile(source);

        int digitsOffset = indexOf(base, LABEL_PREFIX.getBytes(StandardCharsets.US_ASCII));
        if (digitsOffset < 0) {
            throw new IllegalStateException("Could not locate label '" + LABEL_PREFIX + "' in bytecode");
        }
        digitsOffset += LABEL_PREFIX.length();

        // Start clean so a stale run with a different count can't leave extra versions behind.
        deleteRecursively(outDir);
        for (int i = 0; i < numVersions; i++) {
            byte[] variant = base.clone();
            byte[] idx = String.format("%05d", i).getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(idx, 0, variant, digitsOffset, 5);
            Path dst = outDir.resolve(String.format("%05d", i)).resolve("demo").resolve("Leaker.class");
            Files.createDirectories(dst.getParent());
            Files.write(dst, variant);
        }
        System.out.println("[generator] wrote " + numVersions + " versions to " + outDir.toAbsolutePath());
    }

    /** Compile the given {@code demo.Leaker} source to a temp dir and return the class bytes. */
    private static byte[] compile(String source) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler available (need a JDK, not a JRE).");
        }
        Path tmp = Files.createTempDirectory("leakgen");
        try {
            Path src = tmp.resolve("Leaker.java");
            Files.writeString(src, source);
            Path out = Files.createDirectories(tmp.resolve("out"));
            int rc = compiler.run(null, null, null, "-d", out.toString(), src.toString());
            if (rc != 0) {
                throw new IllegalStateException("Template compilation failed (javac rc=" + rc + ")");
            }
            return Files.readAllBytes(out.resolve("demo").resolve("Leaker.class"));
        } finally {
            deleteRecursively(tmp);
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
