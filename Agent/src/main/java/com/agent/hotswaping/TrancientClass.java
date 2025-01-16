package com.agent.hotswaping;

import com.agent.HotSwapAgent;

import java.lang.instrument.UnmodifiableClassException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TrancientClass {
    public static  final ConcurrentMap<String,TrancientClass> avilableClasses = new ConcurrentHashMap<>();
    private final byte[] compiledClass;
    private final String newClassFile;
    private final String className;
    private final Class<?> classRef;
    private volatile boolean classReloaded = false; //TODO fix mulithreading logic

    private TrancientClass(byte[] compiledClass, String newClassFile, String className) throws ClassNotFoundException {
        this.compiledClass = compiledClass;
        this.newClassFile = newClassFile;
        this.className = className;
        this.classRef = Class.forName(className.replace("/", ".")); //TODO this might need to use a classLoader
    }

    public static TrancientClass scedualClassChange(byte[] compiledClass, String newClassFile, String className) throws ClassNotFoundException, UnmodifiableClassException {
        TrancientClass trancientClass = new TrancientClass(compiledClass, newClassFile, className);
        avilableClasses.put(className, trancientClass);
        HotSwapAgent.instrumentationObject.retransformClasses(trancientClass.classRef);
        return trancientClass;
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
}
