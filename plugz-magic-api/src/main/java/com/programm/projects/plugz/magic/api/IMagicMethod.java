package com.programm.projects.plugz.magic.api;

public interface IMagicMethod {

    Object call(Object... arguments) throws MagicInstanceException;

}
