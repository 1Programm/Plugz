package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicInstanceException;

public interface MagicMethod {

    Object invoke(Object... args) throws MagicInstanceException;

}
