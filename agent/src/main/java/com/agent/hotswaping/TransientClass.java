package com.agent.hotswaping;

import com.agent.HotSwapAgent;
import com.agent.TransientCLassInterface;

import java.lang.instrument.UnmodifiableClassException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TransientClass implements TransientCLassInterface {
    public static  final ConcurrentMap<String, TransientClass> avilableClasses = new ConcurrentHashMap<>();
    private final byte[] compiledClass;
    private final String newClassFile;
    private final String className;
    private final Class<?> classRef;
    private final Object lock = new Object();
    private volatile boolean classReloaded = false;

    private TransientClass(byte[] compiledClass, String newClassFile, String className) throws ClassNotFoundException {
        this.compiledClass = compiledClass;
        this.newClassFile = newClassFile;
        this.className = className;
        this.classRef = Class.forName(classNameToJavaFormate(className)); //Not sure if this is always working might need to use the correct class loader in some cases
    }

    public static TransientClass scheduleClassChange(byte[] compiledClass, String newClassFile, String className) throws ClassNotFoundException, UnmodifiableClassException {
        TransientClass transientClass = new TransientClass(compiledClass, newClassFile, className);
        avilableClasses.put(className, transientClass);
        HotSwapAgent.instrumentationObject.retransformClasses(transientClass.classRef);
        return transientClass;
    }

    public byte[] getCompiledClass() {
        return compiledClass;
    }

    public String getNewClassFile() {
        return newClassFile;
    }

    public String getClassName() {
        return className;
    }

    public Class<?> getClassRef() {
        return classRef;
    }

    @Override
    public void waitUntilReloaded() throws InterruptedException {
        synchronized (lock) {
            while (!classReloaded) {
                lock.wait();
            }
        }
    }

    @Override
    public void classReloadDone() {
        synchronized (lock) {
            classReloaded = true;
            lock.notifyAll();
        }
    }

    private static String classNameToJavaFormate(String className) {
        return className.replace("-", ".").replace("/", ".");
    }
}
