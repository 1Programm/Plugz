package com.programm.plugz.codegen.codegenerator;

/**
 * Class representing some java modifiers.
 */
public enum Modifier {

    STATIC("static"),
    FINAL("final"),
    ABSTRACT("abstract"),
    NATIVE("native")

    ;

    public final String value;

    Modifier(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
