package com.programm.projects.plugz.magic;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class SchedulerMethodConfig {
    boolean started;
    long startedAt;
    long startedLast;
    final long startAfter;
    final long repeatAfter;
    final long stopAfter;
    final String beanString;

    abstract void run() throws MagicInstanceException;
}