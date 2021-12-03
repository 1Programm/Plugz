package com.programm.projects.plugz.magic.api;

import com.programm.projects.plugz.magic.MagicInstanceException;

public interface IMagicMethod {

    Object call(Object... arguments) throws MagicInstanceException;

}
