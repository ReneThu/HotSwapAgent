package com.agent.transformer;

import com.agent.capturing.ClassStore;
import com.agent.hotswaping.TransientClass;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class HotSwapTransformer implements ClassFileTransformer {

    private final ClassStore classStore;

    public HotSwapTransformer(ClassStore classStore) {
        this.classStore = classStore;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        TransientClass trancientClass = TransientClass.avilableClasses.get(className);

        if (trancientClass == null) {
            return null;
        }

        try {
            classStore.updateClassFile(trancientClass);
            trancientClass.classReloadDone();
        } catch (IOException e) {
            //TODO log warning!
        }
        return trancientClass.getCompiledClass();
    }
}
