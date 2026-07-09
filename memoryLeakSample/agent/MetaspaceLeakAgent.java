package agent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;

/**
 * Minimal Java agent whose only job is to hand out the {@link Instrumentation} instance and perform
 * class redefinitions on demand. The sample application ({@code demo.Leaker}) calls
 * {@link #redefine(Class, byte[])} via reflection on every loop iteration to hot-swap itself.
 *
 * <p>Each {@link Instrumentation#redefineClasses} call allocates fresh Metaspace for the new class
 * version; driven in a tight loop this is what produces the Metaspace leak the demo showcases.
 */
public final class MetaspaceLeakAgent {

    /** Captured in {@link #premain} and read reflectively by the sample application. */
    public static volatile Instrumentation instrumentation;

    private MetaspaceLeakAgent() {
    }

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
        System.out.println("[agent] MetaspaceLeakAgent attached; redefine supported="
                + inst.isRedefineClassesSupported());
    }

    /**
     * Hot-swap {@code target} to {@code newBytecode}. Invoked via reflection from the sample app so
     * that the app needs no compile-time dependency on this agent.
     */
    public static void redefine(Class<?> target, byte[] newBytecode) throws Exception {
        instrumentation.redefineClasses(new ClassDefinition(target, newBytecode));
    }
}
