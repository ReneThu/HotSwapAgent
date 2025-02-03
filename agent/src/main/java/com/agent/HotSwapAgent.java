package com.agent;

import com.agent.capturing.ClassStore;
import com.agent.classloader.JarClassLoader;
import com.agent.hotswaping.ClassHotSwaper;
import com.agent.transformer.ClassWatcherTransformer;
import com.agent.transformer.HotSwapTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Path;

public class HotSwapAgent {

    public static final ClassStore CLASS_STORE = new ClassStore();
    public static JarClassLoader jarClassLoader;
    public static Instrumentation instrumentationObject;

    private static MicronautApplicationInterface springMain;
    private static ClassHotSwaper classHotSwaper;
    private static String[] args;

    public static void premain(String arguments, Instrumentation instrumentation) {
        args = arguments.split(",");

        if (args.length != 2) {
            System.err.println("Incorrect number of arguments, agent shutting off");
        }
        String classPattern = args[0];
        try {
            startWebServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        instrumentationObject = instrumentation;
        instrumentation.addTransformer(new ClassWatcherTransformer(classPattern, CLASS_STORE), false);
        instrumentation.addTransformer(new HotSwapTransformer(CLASS_STORE), true);
    }

    public static void startWebServer() throws Exception {
        URL[] urls = new URL[] { Path.of(args[1]).toUri().toURL()};

        //The mirconaut app could also be packaged together with the agent then no classLoader would be needed.
        jarClassLoader = new JarClassLoader(urls, ClassLoader.getSystemClassLoader());
        Class<?> springClass = jarClassLoader.loadClass("example.micronaut.Application");
        Constructor<?> constructor = springClass.getConstructor();
        springMain =  (MicronautApplicationInterface) constructor.newInstance();
        classHotSwaper = new ClassHotSwaper();
        springMain.start(CLASS_STORE, classHotSwaper);
    }
}
