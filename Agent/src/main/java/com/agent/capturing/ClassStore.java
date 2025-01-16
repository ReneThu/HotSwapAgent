package com.agent.capturing;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import com.agent.ClassStoreInterface;
import com.agent.hotswaping.TrancientClass;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

public class ClassStore implements ClassStoreInterface {

    private final Map<String, SavedClassFile> classToFileMap = new HashMap<>();
    private final String tmpFolder = "/tmp"; //TODO look this up
    public void saveClass(String className, byte[] classfileBuffer) {
        classToFileMap.put(className, new SavedClassFile(className, classfileBuffer));
    }

    public void decompileSavedClass(List<String> className) {
        String[] args = new String[className.size() + 2];
        args[0] = "-jrt=1"; // use current runtime
        for (int i = 0; i < className.size(); i++) {
            args[i + 1] = classToFileMap.get(className.get(i)).getClassFileLocation().toString();
        }
        args[className.size() + 1] = tmpFolder;
        ConsoleDecompiler.main(args);
        className.forEach(s -> classToFileMap.get(s).addDecompiledFile());
    }

    public Optional<String> getDecompiledClassAsString(String className) throws IOException {
        SavedClassFile savedClassFile = classToFileMap.get(className);

        if (savedClassFile == null) {
            return Optional.empty();
        }
        return Optional.of(new String(Files.readAllBytes(savedClassFile.getDecompiledClassLocation())));
    }

    public Set<String> getAllCollectedClasses() {
        return classToFileMap.keySet();
    }

    public void updateClassFile(TrancientClass trancientClass) throws IOException {
        SavedClassFile savedClassFile = classToFileMap.get(trancientClass.getClassName());
        savedClassFile.updateDecompiledClassLocation(trancientClass.getNewClassFile());
        savedClassFile.updateClassFileLocation(trancientClass.getCompiledClass());
    }

}
