package com.agent.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class JarClassLoader extends URLClassLoader {

    private String jarPath;

    public JarClassLoader(URL[] resource, ClassLoader parent) throws Exception {
        super(resource, parent);
        jarPath = resource[0].getPath();
        System.out.println(jarPath);
    }
}
