package com.programm.projects.plugz.magic.api;

public interface IAsyncManager {

    void shutdown();

    void runAsyncVipTask(Runnable task, long delay);

    void runAsyncTask(Runnable task, long delay);

}
