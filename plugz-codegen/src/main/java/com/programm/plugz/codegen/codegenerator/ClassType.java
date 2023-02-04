package com.programm.plugz.codegen.codegenerator;

/**
 * Class representing types of java classes.
 */
public enum ClassType {

    CLASS("class"),
    INTERFACE("interface"),
    ENUM("enum")

    ;

    public final String type;

    ClassType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
