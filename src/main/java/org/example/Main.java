package org.example;

public class Main {
    public static void main(String[] args) throws Exception {
        while (true) {
            Thread.sleep(1000);
            printHelloWorld();
        }
    }

    public static void printHelloWorld() {
        System.out.println("Hello, World!");
    }
}