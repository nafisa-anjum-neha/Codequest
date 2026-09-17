package com.codequest;

/**
 * Launcher bypasses the JavaFX 11+ requirement that the main class
 * cannot directly extend javafx.application.Application when run from the classpath.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
