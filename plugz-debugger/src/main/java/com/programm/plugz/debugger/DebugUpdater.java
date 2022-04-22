package com.programm.plugz.debugger;

class DebugUpdater {

    private final DebuggerWindow window;
    private final long fpsSleepMillis;
    private final long longSleepMillis;

    private Thread runningThread;
    private boolean running;

    public DebugUpdater(DebuggerWindow window, long fpsSleepMillis, long longSleepMillis) {
        this.window = window;
        this.fpsSleepMillis = fpsSleepMillis;
        this.longSleepMillis = longSleepMillis;
    }

    public void startup(){
        this.runningThread = Thread.currentThread();
        this.running = true;
        try {
            while(running){
                if(window.hasFPSValues()){
                    window.updateFPSValues();
                    Thread.sleep(fpsSleepMillis);
                }
                else {
                    Thread.sleep(longSleepMillis);
                }
            }
        }
        catch (InterruptedException e){
            this.running = false;
        }
    }

    public void shutdown(){
        running = false;
        runningThread.interrupt();
    }
}
