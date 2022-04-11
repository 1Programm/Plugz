package com.programm.plugz.api;

public interface IAsyncManager {

    void runAsyncVipTask(Runnable task, long delay);

    void runAsyncTask(Runnable task, long delay);

}
