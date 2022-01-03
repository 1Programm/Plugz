package com.programm.projects.plugz.magic.api;

public abstract class ScheduledMethodConfig {

    public boolean started;
    public long startedAt;
    public long startedLast;
    public final long startAfter;
    public final long repeatAfter;
    public final long stopAfter;
    public final String beanString;
    public final Async async;

    public ScheduledMethodConfig(long startAfter, long repeatAfter, long stopAfter, String beanString, Async async) {
        this.startAfter = startAfter;
        this.repeatAfter = repeatAfter;
        this.stopAfter = stopAfter;
        this.beanString = beanString;
        this.async = async;
    }

    public abstract void run() throws MagicInstanceException;
}