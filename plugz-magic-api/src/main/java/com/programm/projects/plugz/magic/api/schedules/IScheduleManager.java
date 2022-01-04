package com.programm.projects.plugz.magic.api.schedules;

import com.programm.projects.plugz.magic.api.ISubsystem;

import java.net.URL;

public interface IScheduleManager extends ISubsystem {

    void start();

    void removeUrl(URL url);

    void scheduleRunnable(URL fromUrl, ScheduledMethodConfig config);

    ISchedules getScheduleHandle();

}
