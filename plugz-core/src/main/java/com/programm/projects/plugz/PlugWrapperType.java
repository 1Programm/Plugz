package com.programm.projects.plugz;

public enum PlugWrapperType {

    LOOSE_CLASSES(1),
    JAR(2),

    ;

    final int val;

    PlugWrapperType(int val){
        this.val = val;
    }

    public boolean contains(int num){
        return (num & val) != 0;
    }

}
