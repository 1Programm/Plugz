package com.programm.projects.plugz.magic.api;

import java.net.URL;

public interface IScheduleManager {

    void startup();

    void shutdown();

    void removeUrl(URL url);

    void scheduleRunnable(URL fromUrl, ScheduledMethodConfig config);

    ISchedules getScheduleHandle();

}
