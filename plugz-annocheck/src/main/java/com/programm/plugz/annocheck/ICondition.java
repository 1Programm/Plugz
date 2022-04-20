package com.programm.plugz.annocheck;

public interface ICondition {

    ICondition set(Class<?> cls);
    ICondition and(Class<?> cls);
    ICondition or(Class<?> cls);
    void seal();

}
