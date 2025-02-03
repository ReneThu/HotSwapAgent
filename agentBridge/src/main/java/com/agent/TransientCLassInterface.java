package com.agent;

public interface TransientCLassInterface {
    void waitUntilReloaded() throws InterruptedException;
    void classReloadDone();
}
