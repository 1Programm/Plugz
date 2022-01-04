package com.programm.projects.plugz.simple.schedules;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.schedules.IScheduleManager;
import com.programm.projects.plugz.magic.api.schedules.ISchedules;
import com.programm.projects.plugz.magic.api.schedules.ScheduledMethodConfig;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Logger("Simple-Scheduler")
public class ScheduleManager implements IScheduleManager, ISchedules, Runnable {

    private static final long MIN_SLEEP = 100L;

    private final Map<URL, List<ScheduledMethodConfig>> schedulerConfigs = new HashMap<>();
    private final Map<String, ScheduledMethodConfig> mappedBeanConfigs = new HashMap<>();
    @Get private ILogger log;
    @Get private IAsyncManager asyncManager;

    private boolean running;
    private boolean paused;

    @Override
    public void run(){
        long now = curTime();
        for(URL url : schedulerConfigs.keySet()) {
            for (ScheduledMethodConfig config : schedulerConfigs.get(url)) {
                config.startedAt = now;
            }
        }

        List<URL> urls = new ArrayList<>();

        try {
            while(running && !paused){
                Thread.sleep(MIN_SLEEP);

                now = curTime();
                urls.clear();
                urls.addAll(schedulerConfigs.keySet());

                for(URL url : urls) {
                    List<ScheduledMethodConfig> configs = schedulerConfigs.get(url);
                    if(configs == null) continue;

                    for (int i = 0; i < configs.size(); i++) {
                        ScheduledMethodConfig config = configs.get(i);
                        if (!config.started) {
                            if (config.startedAt + config.startAfter < now) {
                                config.started = true;
                                config.startedAt = now;
                                config.startedLast = now;
                                runConfig(config);
                                if (config.repeatAfter <= 0) {
                                    configs.remove(i);
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
                                configs.remove(i);
                                i--;
                            }
                        }
                    }
                }

                int runningConfigs = 0;
                for(URL url : schedulerConfigs.keySet()){
                    List<ScheduledMethodConfig> configs = schedulerConfigs.get(url);
                    runningConfigs += configs == null ? 0 : configs.size();
                }

                if(runningConfigs == 0){
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

    @Override
    public void startup(){}

    @Override
    public void shutdown(){
        running = false;
    }

    @Override
    public void start() {
        if(running) return;
        running = true;
        paused = false;


        log.info("Started.");
        asyncManager.runAsyncVipTask(this, 0);
    }

    @Override
    public void scheduleRunnable(URL fromUrl, ScheduledMethodConfig config){
        log.debug("Scheduling method: [{}].", config.beanString);

        if(config.repeatAfter < MIN_SLEEP){
            log.warn("Scheduled method ({}) cannot run faster than every {} milliseconds!", config.beanString, MIN_SLEEP);
        }

        schedulerConfigs.computeIfAbsent(fromUrl, url -> new ArrayList<>()).add(config);
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
    public void removeUrl(URL url){
        log.debug("Removing url: [{}].", url);

        List<ScheduledMethodConfig> configs = schedulerConfigs.remove(url);

        if(configs != null){
            for(ScheduledMethodConfig config : configs){
                mappedBeanConfigs.remove(config.beanString);
            }

            log.debug("Removed {} scheduled methods.", configs.size(), url);
        }
    }

    @Override
    public ISchedules getScheduleHandle() {
        return this;
    }

    @Override
    public void stopScheduler() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[2];
        String beanName = element.getClassName() + "#" + element.getMethodName();
        ScheduledMethodConfig config = mappedBeanConfigs.remove(beanName);

        if(config == null){
            throw new IllegalStateException("No scheduler for bean with name: [" + beanName + "] found!");
        }

        for(URL url : schedulerConfigs.keySet()) {
            List<ScheduledMethodConfig> configs = schedulerConfigs.get(url);
            if(configs.remove(config)) break;
        }
    }

    @Override
    public String toString() {
        return "Scheduler";
    }
}
