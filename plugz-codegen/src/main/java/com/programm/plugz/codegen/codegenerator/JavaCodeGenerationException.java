package com.programm.plugz.codegen.codegenerator;

public class JavaCodeGenerationException extends Exception {

    public JavaCodeGenerationException(String message, Throwable e) {
        super(message, e);
    }

    public JavaCodeGenerationException(String message, StringBuilder sb) {
        super(message + "\n" + sb);
    }
}
