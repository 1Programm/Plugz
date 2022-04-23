package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicInstanceException;

public interface MagicMethod {

    int argsCount();

    int magicArgsCount();

    int nonMagicArgsCount();

    Object invoke(Object... args) throws MagicInstanceException;

}
