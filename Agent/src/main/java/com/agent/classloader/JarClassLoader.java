package com.agent.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class JarClassLoader extends URLClassLoader {

    private String jarPath;

    public JarClassLoader(URL[] resource, ClassLoader parent) throws Exception {
        super(resource, parent);
        jarPath = resource[0].getPath();
        System.out.println("TESTETSTESTESTEST");
    }

//    @Override
//    public Class<?> findClass(String name) throws ClassNotFoundException {
//        try {
//            File jarFile = new File(jarPath);
//            try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {
//                JarEntry jarEntry;
//                while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
//                    if (jarEntry.getName().endsWith(".class")) {
//                        String className = jarEntry.getName().replace("/", ".").replace(".class", "");
//                        if (className.equals(name)) {
//                            byte[] classBytes = jarInputStream.readAllBytes();
//                            return defineClass(name, classBytes, 0, classBytes.length);
//                        }
//                    }
//                }
//            }
//        } catch (IOException e) {
//            throw new ClassNotFoundException("Class " + name + " not found in JAR file " + jarPath, e);
//        }
//        throw new ClassNotFoundException("Class " + name + " not found");
//    }
}
