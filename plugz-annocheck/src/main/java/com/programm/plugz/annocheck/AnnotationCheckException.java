package com.programm.plugz.annocheck;

public class AnnotationCheckException extends Exception {

    public AnnotationCheckException(String message) {
        super(message);
    }

    public AnnotationCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnnotationCheckException(Throwable cause) {
        super(cause);
    }
}
