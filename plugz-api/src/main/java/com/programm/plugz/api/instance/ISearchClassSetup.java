package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicInstanceException;

public interface ISearchClassSetup {

    void setup(Class<?> implementingCls, IInstanceManager manager) throws MagicInstanceException;
}
