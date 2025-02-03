package com.agent.hotswaping;

import com.agent.ClassHotSwapInterface;
import com.agent.TransientCLassInterface;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ClassHotSwaper implements ClassHotSwapInterface {

    @Override
    public TransientCLassInterface hotSwap(String fullCLassName, String classFile) throws ClassNotFoundException, UnmodifiableClassException {
        byte[] compiledBydeCode = compile(fullCLassName, classFile);
        return TransientClass.scheduleClassChange(compiledBydeCode, classFile, fullCLassName);
    }


    public static byte[] compile(String className, String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(diagnostics, null, null));

        JavaFileObject file = new InMemoryJavaFileObject(className, sourceCode);
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);

        //TODO This would require access to the other files to compile any file on the fly.
        //There are multiple ways this could be achieved. The system classLoader should have access to most of them in most
        //cases, but accessing that might be impossible without providing a custom classLoader on start-up and tbh
        //I was too lazy to do that. A different way might be to analyze to
        //the provided source code and use the decompiler to generate recursively
        //The source files until all needed files are present to compile against that.
        //There might also be a different way, but I did not spend too much time on this.
//        List<String> options = Arrays.asList("-classpath", getSystemLoaderUrls());
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

        boolean success = task.call();
        if (success) {
            return fileManager.getClassBytes().get(className.replace("/", ".")).outputStream.toByteArray();
        } else {
            diagnostics.getDiagnostics().forEach(diagnostic -> {
                System.out.println(diagnostic.getMessage(null));
            });
            return null;
        }
    }

    //This does not work with newer java version
    private static String getSystemLoaderUrls() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp =  ucpField.get(systemClassLoader);

            Method getURLsMethod = ucp.getClass().getDeclaredMethod("getURLs");
            getURLsMethod.setAccessible(true);

            URL[] urls = (URL[]) getURLsMethod.invoke(ucp);

            for (URL url : urls) {
                System.out.println(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        protected InMemoryJavaFileObject(String className, String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    }

    static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, ByteArrayJavaFileObject> classBytes = new HashMap<>();

        protected InMemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            ByteArrayJavaFileObject fileObject = new ByteArrayJavaFileObject(className, kind);
            classBytes.put(className, fileObject);
            return fileObject;
        }

        public Map<String, ByteArrayJavaFileObject> getClassBytes() {
            return classBytes;
        }
    }

    static class ByteArrayJavaFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        protected ByteArrayJavaFileObject(String className, Kind kind) {
            super(URI.create("bytes:///" + className.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return outputStream;
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }
}
