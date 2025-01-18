package com.agent;

import java.lang.instrument.UnmodifiableClassException;

public interface ClassHotSwapInterface {
    TransiendtCLassInterface hotSwap(String fullCLassName, String classFile) throws ClassNotFoundException, UnmodifiableClassException;
}
