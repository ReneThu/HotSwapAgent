package com.agent.capturing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SavedClassFile {
    private final String className;
    private Path classFileLocation;
    private Path decompiledClassLocation;

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
        decompiledClassLocation = Path.of(classFileLocation.toString().replaceFirst("class", "java"));
    }

    public Path getClassFileLocation() {
        return classFileLocation;
    }

    public Path getDecompiledClassLocation() {
        return decompiledClassLocation;
    }

    public void updateDecompiledClassLocation(String file) throws IOException {
        this.classFileLocation = Files.createTempFile(null, ".class");
        Files.writeString(this.classFileLocation, file);
    }

    public void updateClassFileLocation(byte[] classfileBuffer) throws IOException {
        this.classFileLocation = Files.createTempFile(null, ".class");
        Files.write(this.classFileLocation, classfileBuffer);
    }
}
