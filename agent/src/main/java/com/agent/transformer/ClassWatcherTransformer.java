package com.agent.transformer;

import com.agent.capturing.ClassStore;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collections;

public class ClassWatcherTransformer implements ClassFileTransformer {

    private final String classPattern;
    private final ClassStore classStore;

    public ClassWatcherTransformer(String classPattern, ClassStore classStore) {
        this.classPattern = classPattern;
        this.classStore = classStore;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className != null && className.startsWith(classPattern)) {
            classStore.saveClass(className, classfileBuffer);
        }
        return null; //This returns null if no transformation is done
    }
}
