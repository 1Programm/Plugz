package com.programm.projects.plugz.magic.api;

public interface IInstanceManager {

    <T> T getInstance(Class<T> cls) throws MagicInstanceException;

    <T> T instantiate(Class<T> cls) throws MagicInstanceException;

}
