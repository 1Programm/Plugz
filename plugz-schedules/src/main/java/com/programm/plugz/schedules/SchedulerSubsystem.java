package com.programm.plugz.schedules;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.api.instance.MagicMethod;

import java.lang.reflect.Method;

@Logger("Scheduler")
public class SchedulerSubsystem implements ISubsystem {

    private static final String CONF_MIN_SLEEP_NAME = "scheduler.min-sleep";
    private static final long CONF_MIN_SLEEP_DEFAULT = 1000;

    private final ILogger log;
    private final ScheduleManager scheduleManager;

    public SchedulerSubsystem(@Get ILogger log, @Get IAsyncManager asyncManager, @Get PlugzConfig config){
        this.log = log;
        long minSleep = config.getLongOrRegisterDefault(CONF_MIN_SLEEP_NAME, CONF_MIN_SLEEP_DEFAULT);
        this.scheduleManager = new ScheduleManager(log, asyncManager, minSleep);
    }

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException{
        setupHelper.registerInstance(ISchedules.class, scheduleManager);

        setupHelper.registerMethodAnnotation(Scheduled.class, this::setupScheduledMethods);

        //Does nothing as @Config classes are already instantiated
        annocheck.forClass(Scheduled.class).classAnnotations().blacklist().set(Config.class);
    }

    @Override
    public void startup() {
        scheduleManager.start();
    }

    @Override
    public void shutdown() {
        scheduleManager.stop();
    }

    private void setupScheduledMethods(Scheduled annotation, Object instance, Method method, IInstanceManager manager, PlugzConfig config) {
        String methodBeanString = instance.getClass().getName() + "#" + method.getName();

        if(annotation.repeat() < scheduleManager.minSleep){
            log.warn("Scheduled method ({}) cannot run faster than every {} milliseconds!", methodBeanString, scheduleManager.minSleep);
            log.warn("You can set the [{}] configuration to something lower if you like.", CONF_MIN_SLEEP_NAME);
        }

        MagicMethod mm = manager.buildMagicMethod(instance, method);
        ScheduledMethodConfig scheduledMagicMethod = new ScheduledMethodConfig(mm, annotation.startAfter(), annotation.repeat(), annotation.stopAfter(), methodBeanString);
        scheduleManager.scheduleRunnable(scheduledMagicMethod);
    }
}
