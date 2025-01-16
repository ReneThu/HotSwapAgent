package com.agent;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ClassStoreInterface {

    void decompileSavedClass(List<String> className);
    Optional<String> getDecompiledClassAsString(String className) throws IOException;
    Set<String> getAllCollectedClasses();
}
