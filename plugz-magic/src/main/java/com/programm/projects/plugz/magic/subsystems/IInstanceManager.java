package com.programm.projects.plugz.magic.subsystems;

import com.programm.projects.plugz.magic.MagicInstanceException;

public interface IInstanceManager {

    <T> T getInstance(Class<T> cls) throws MagicInstanceException;

    <T> T instantiate(Class<T> cls) throws MagicInstanceException;

}
