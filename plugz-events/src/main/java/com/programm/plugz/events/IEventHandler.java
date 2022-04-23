package com.programm.plugz.events;

public interface IEventHandler {

    void emit(String name, Object... args);

    IEventSubscription subscribe(String name, IEventListener listener);

}
