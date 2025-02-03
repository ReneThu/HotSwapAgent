package com.agent;

import java.lang.instrument.UnmodifiableClassException;

public interface ClassHotSwapInterface {
    TransientCLassInterface hotSwap(String fullCLassName, String classFile) throws ClassNotFoundException, UnmodifiableClassException;
}
