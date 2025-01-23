package com.agent.capturing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SavedClassFile {
    private final String className;
    private Path classFileLocation;
    private Path decompiledClassLocation;
    private boolean hasBeenDecompiled = false;

    public SavedClassFile(String className, byte[] classfileBuffer) {
        this.className = className;
        try {
            this.classFileLocation = Files.createTempFile(null, ".class");
            Files.write(this.classFileLocation, classfileBuffer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save class.");
        }
    }

    public void addDecompiledFile() {
        hasBeenDecompiled = true;
        decompiledClassLocation = Path.of(classFileLocation.toString().replaceFirst("class", "java"));
    }

    public Path getClassFileLocation() {
        return classFileLocation;
    }

    public Path getDecompiledClassLocation() {
        return decompiledClassLocation;
    }

    public void updateDecompiledClassLocation(String file) throws IOException {
        this.decompiledClassLocation = Files.createTempFile(null, ".class");
        Files.writeString(this.decompiledClassLocation, file);
    }

    public void updateClassFileLocation(byte[] classfileBuffer) throws IOException {
        this.classFileLocation = Files.createTempFile(null, ".class");
        Files.write(this.classFileLocation, classfileBuffer);
    }

    public boolean isHasBeenDecompiled() {
        return hasBeenDecompiled;
    }
}
