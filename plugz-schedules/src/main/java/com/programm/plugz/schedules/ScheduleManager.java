package com.programm.plugz.schedules;

import com.programm.plugz.api.IAsyncManager;
import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.MagicRuntimeException;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Logger("Scheduler")
public class ScheduleManager implements Runnable, ISchedules {

    private static final long MIN_SLEEP = 100L;

    private final List<ScheduledMethodConfig> schedulerConfigs = new ArrayList<>();
    private final Map<String, ScheduledMethodConfig> mappedBeanConfigs = new HashMap<>();
    private final ILogger log;
    private final IAsyncManager asyncManager;

    private boolean running;
    private boolean paused;

    @Override
    public void run(){
        long now = curTime();
        for(ScheduledMethodConfig config : schedulerConfigs) {
            config.startedAt = now;
        }

        try {
            while(running && !paused){
                Thread.sleep(MIN_SLEEP);

                now = curTime();

                for (int i=0;i<schedulerConfigs.size();i++) {
                    ScheduledMethodConfig config = schedulerConfigs.get(i);
                    if (!config.started) {
                        if (config.startedAt + config.startAfter < now) {
                            config.started = true;
                            config.startedAt = now;
                            config.startedLast = now;
                            runConfig(config);
                            if (config.repeatAfter <= 0) {
                                schedulerConfigs.remove(i);
                                i--;
                            }
                        }
                    } else {
                        if (config.startedLast + config.repeatAfter < now) {
                            config.startedLast = now;
                            runConfig(config);
                        }
                    }

                    if (config.stopAfter != 0) {
                        if (config.startedAt + config.stopAfter < now) {
                            schedulerConfigs.remove(i);
                            i--;
                        }
                    }
                }

                if(schedulerConfigs.isEmpty()){
                    paused = true;
                }
            }
        }
        catch (InterruptedException e){
            log.debug("Interrupted and will shut down.");
        }

        if(!running) {
            log.info("Shutdown.");
        }
        else {
            log.info("Paused and will wake up if a new Scheduled method is detected.");
        }
    }

    private void runConfig(ScheduledMethodConfig config) {
        try {
            config.run();
        } catch (MagicInstanceException e) {
            throw new MagicRuntimeException(e);
        }
    }

    private long curTime(){
        return System.currentTimeMillis();
    }

    public void start() {
        if(running) return;
        running = true;
        paused = false;


        log.info("Started.");
        asyncManager.runAsyncVipTask(this, 0);
    }

    public void stop(){
        running = false;
    }

    public void scheduleRunnable(ScheduledMethodConfig config){
        log.debug("Scheduling method: [{}].", config.beanString);

        if(config.repeatAfter < MIN_SLEEP){
            log.warn("Scheduled method ({}) cannot run faster than every {} milliseconds!", config.beanString, MIN_SLEEP);
        }

        schedulerConfigs.add(config);
        mappedBeanConfigs.put(config.beanString, config);

        if(running && paused) {
            paused = false;
            log.info("Restarting paused thread for scheduling.");
            asyncManager.runAsyncVipTask(this, 0);
        }

        if(running){
            config.startedAt = curTime();
        }
    }

    @Override
    public void stopSchedule() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[2];
        String beanName = element.getClassName() + "#" + element.getMethodName();
        ScheduledMethodConfig config = mappedBeanConfigs.remove(beanName);

        if(config == null){
            throw new IllegalStateException("No scheduler for bean with name: [" + beanName + "] found!");
        }

        schedulerConfigs.remove(config);
    }

    @Override
    public String toString() {
        return "Scheduler";
    }

}
