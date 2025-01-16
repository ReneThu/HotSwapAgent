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

    public static void premain(String arguments, Instrumentation instrumentation) {
        //TODO get class name from arguments
        String classPattern = "org/example";
        try {
            startWebServer();
        } catch (Exception e) {
            //TODO log warning could not start web serevr
            throw new RuntimeException(e);
        }

        instrumentationObject = instrumentation;
        instrumentation.addTransformer(new ClassWatcherTransformer(classPattern, CLASS_STORE), false);
        instrumentation.addTransformer(new HotSwapTransformer(CLASS_STORE), true);
    }

    public static void startWebServer() throws Exception {
        URL[] urls = new URL[]
                {
                        //TODO write custom class loader that can read from jar.ressource
                        Path.of("/home/marco/Documents/Development/techEvangelistGeneric/HotSwapAgentV2/Micronaut/build/libs/Micronaut-0.1-all.jar").toUri().toURL(),
                };

        jarClassLoader = new JarClassLoader(urls, ClassLoader.getSystemClassLoader());
        Class<?> springClass = jarClassLoader.loadClass("example.micronaut.Application");
        Constructor<?> constructor = springClass.getConstructor();
        springMain =  (MicronautApplicationInterface) constructor.newInstance();
        classHotSwaper = new ClassHotSwaper();
        springMain.start(CLASS_STORE, classHotSwaper);
    }
}
