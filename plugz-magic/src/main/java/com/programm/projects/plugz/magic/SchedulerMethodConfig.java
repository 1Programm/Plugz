package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.magic.api.Async;
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
    final Async async;

    protected abstract void run() throws MagicInstanceException;
}