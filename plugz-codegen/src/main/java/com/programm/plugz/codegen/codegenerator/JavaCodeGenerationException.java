package com.programm.plugz.codegen.codegenerator;

/**
 * Exception used to hint the user that the java code would be invalid and could not be compiled when generating a java class.
 */
public class JavaCodeGenerationException extends Exception {

    public JavaCodeGenerationException(String message, Throwable e) {
        super(message, e);
    }

    public JavaCodeGenerationException(String message, StringBuilder sb) {
        super(message + "\n" + sb);
    }
}
