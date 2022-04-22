package com.programm.plugz.api;

public interface IAsyncManager {

    default void runAsyncTask(Runnable task, long delay){
        runAsyncTask(task, null, delay, false, false);
    }

    default void runAsyncTask(Runnable task, Runnable onTaskFinished, long delay){
        runAsyncTask(task, onTaskFinished, delay, false, false);
    }

    default void runAsyncTask(Runnable task, long delay, boolean weakThread){
        runAsyncTask(task, null, delay, false, weakThread);
    }

    /**
     * Method to run a task (Runnable) on a new Thread asynchronously.
     * @param task the Task which should be run asynchronously.
     * @param onTaskFinished a callback method when the task was finished OR exited because of some reasons.
     * @param delay some delay. Must be greater or equal to 0.
     * @param vip flag which specifies if the task should be prioritised.
     * @param weakThread flag to specify if the worker thread should be weak.
     *                   A weak worker thread will exit early if all non - weak threads have exited and the environment agreed to exit.
     */
    void runAsyncTask(Runnable task, Runnable onTaskFinished, long delay, boolean vip, boolean weakThread);

    default void notifyCurrentNewThread(){
        String name = Thread.currentThread().getName();
        notifyNewThread(name);
    }

    /**
     * Notifies the Auto-Shutdown system that a thread was started outside the environment and should be accounted for when handling auto closing.
     * Ex.: If you have some Swing UI and you want the environment to stay alive as long as it is not closed
     * you should pass the name of the AWT-Thread here and later use the notifyThreadClose method to tell the environment that the awt thread stopped (or will stop).
     * @param threadName the name of the thread.
     */
    void notifyNewThread(String threadName);


    default void notifyCurrentThreadClose(){
        String name = Thread.currentThread().getName();
        notifyThreadClose(name);
    }

    /**
     * Notifies the Auto-Shutdown system that some thread stopped running.
     * When all non - weak environment threads have stopped running the environment will be shutting down itself.
     * @param threadName the name of the thread that stopped running.
     */
    void notifyThreadClose(String threadName);

}
