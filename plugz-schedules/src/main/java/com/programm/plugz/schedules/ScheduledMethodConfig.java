package com.programm.plugz.schedules;

import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.instance.MagicMethod;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScheduledMethodConfig {

    public final MagicMethod mm;
    public final long startAfter;
    public final long repeatAfter;
    public final long stopAfter;
    public final String beanString;

    public boolean started;
    public long startedAt;
    public long startedLast;

    public void run() throws MagicInstanceException {
        mm.invoke();
    }

}
