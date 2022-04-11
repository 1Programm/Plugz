package com.programm.plugz.schedules;

import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.api.instance.MagicMethod;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;

import java.lang.reflect.Method;

@Logger("Scheduler")
public class SchedulerSubsystem implements ISubsystem {

    private final ILogger log;
    private final ScheduleManager scheduleManager;

    public SchedulerSubsystem(@Get ILogger log, @Get IAsyncManager asyncManager){
        this.log = log;
        this.scheduleManager = new ScheduleManager(log, asyncManager);
    }

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException{
        setupHelper.registerInstance(ISchedules.class, scheduleManager);

        setupHelper.registerMethodAnnotation(Scheduled.class, this::setupScheduledMethods);

        //Does nothing as @Config classes are already instantiated
        annocheck.blacklistClassAnnotations(Scheduled.class).set(Config.class);
    }

    @Override
    public void startup() throws MagicException {
        log.debug("Starting scheduler...");
        scheduleManager.start();
    }

    @Override
    public void shutdown() throws MagicException {
        log.info("Stopping the scheduler...");
        scheduleManager.stop();
    }

    private void setupScheduledMethods(Scheduled annotation, Object instance, Method method, IInstanceManager manager, PlugzConfig config) {
        log.trace("Registering schedule method: {}", method);
        MagicMethod mm = manager.buildMagicMethod(instance, method);
        ScheduledMethodConfig scheduledMagicMethod = new ScheduledMethodConfig(mm, annotation.startAfter(), annotation.repeat(), annotation.stopAfter(), instance.getClass().getName() + "#" + method.getName());
        scheduleManager.scheduleRunnable(scheduledMagicMethod);
    }
}
