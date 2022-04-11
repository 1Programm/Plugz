package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicException;

public interface MagicConsumer<T> {

    void accept(T obj) throws MagicException;

}
