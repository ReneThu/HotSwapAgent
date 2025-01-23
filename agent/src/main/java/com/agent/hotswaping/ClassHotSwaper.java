package com.agent.hotswaping;

import com.agent.ClassHotSwapInterface;
import com.agent.HotSwapAgent;
import com.agent.TransiendtCLassInterface;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassDefinition;
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
    public TransiendtCLassInterface hotSwap(String fullCLassName, String classFile) throws ClassNotFoundException, UnmodifiableClassException {
        byte[] compiledBydeCode = compile(fullCLassName, classFile);
        return TrancientClass.scedualClassChange(compiledBydeCode, classFile, fullCLassName);
    }


    public static byte[] compile(String className, String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(diagnostics, null, null));

        JavaFileObject file = new InMemoryJavaFileObject(className, sourceCode);
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);

        //TODO add jar paths for comiling of systemLoadedClasses.
//        List<String> options = Arrays.asList("-classpath", getSystemLoaderUrls());
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

        boolean success = task.call();
        if (success) {

            //TODO exmplain the replace all
            return fileManager.getClassBytes().get(className.replace("/", ".")).outputStream.toByteArray();
        } else {
            diagnostics.getDiagnostics().forEach(diagnostic -> {
                System.out.println(diagnostic.getMessage(null));
            });
            return null;
        }
    }

    private static String getSystemLoaderUrls() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            // Get the system class loader
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

            // Access the private field 'ucp' in URLClassLoader
            Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
            ucpField.setAccessible(true);

            // Get the value of the 'ucp' field
            Object ucp =  ucpField.get(systemClassLoader);

            Method getURLsMethod = ucp.getClass().getDeclaredMethod("getURLs");
            getURLsMethod.setAccessible(true);

            // Invoke the 'getURLs' method
            URL[] urls = (URL[]) getURLsMethod.invoke(ucp);


            // Print the URLs in the URLClassPath
            System.out.println("URLs in the URLClassPath:");
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
