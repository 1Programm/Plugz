package com.programm.plugz.codegen.codegenerator;

public enum Visibility {

    PACKAGE_PRIVATE(""),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public")

    ;

    public final String value;

    Visibility(String value) {
        this.value = value;
    }

    public void withSpace(StringBuilder sb){
        sb.append(value);
        if(this != PACKAGE_PRIVATE) sb.append(" ");
    }

    @Override
    public String toString() {
        return value;
    }
}
