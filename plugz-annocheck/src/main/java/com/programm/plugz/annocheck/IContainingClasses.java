package com.programm.plugz.annocheck;

public interface IContainingClasses {

    void whitelist(Class<?>... classes);
    void blacklist(Class<?>... classes);

}
