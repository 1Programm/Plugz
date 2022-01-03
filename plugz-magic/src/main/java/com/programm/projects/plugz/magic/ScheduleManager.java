package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.ISchedules;
import com.programm.projects.plugz.magic.api.MagicInstanceException;
import com.programm.projects.plugz.magic.api.MagicRuntimeException;
import lombok.RequiredArgsConstructor;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Logger("Scheduler")
class ScheduleManager implements ISchedules, Runnable {

    private static final long MIN_SLEEP = 100L;

    private final Map<URL, List<SchedulerMethodConfig>> schedulerConfigs = new HashMap<>();
    private final Map<String, SchedulerMethodConfig> mappedBeanConfigs = new HashMap<>();
    private final ILogger log;
    private final ThreadPoolManager threadPoolManager;

    private boolean running;
    private boolean paused;

    @Override
    public void run(){
        long now = curTime();
        for(URL url : schedulerConfigs.keySet()) {
            for (SchedulerMethodConfig config : schedulerConfigs.get(url)) {
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
                    List<SchedulerMethodConfig> configs = schedulerConfigs.get(url);
                    if(configs == null) continue;

                    for (int i = 0; i < configs.size(); i++) {
                        SchedulerMethodConfig config = configs.get(i);
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
                    List<SchedulerMethodConfig> configs = schedulerConfigs.get(url);
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

    private void runConfig(SchedulerMethodConfig config) {
        try {
            config.run();
        } catch (MagicInstanceException e) {
            throw new MagicRuntimeException(e);
        }
    }

    private long curTime(){
        return System.currentTimeMillis();
    }

    public void startup(){
        if(running) return;
        running = true;
        paused = false;


        log.info("Started.");
        threadPoolManager.runAsyncVipTask(this, 0);
    }

    public void shutdown(){
        running = false;
    }

    public void scheduleRunnable(URL fromUrl, SchedulerMethodConfig config){
        log.debug("Scheduling method: [{}].", config.beanString);

        if(config.repeatAfter < MIN_SLEEP){
            log.warn("Scheduled method ({}) cannot run faster than every {} milliseconds!", config.beanString, MIN_SLEEP);
        }

        schedulerConfigs.computeIfAbsent(fromUrl, url -> new ArrayList<>()).add(config);
        mappedBeanConfigs.put(config.beanString, config);

        if(running && paused) {
            paused = false;
            log.info("Restarting paused thread for scheduling.");
            threadPoolManager.runAsyncVipTask(this, 0);
        }


        if(running){
            config.startedAt = curTime();
        }
    }

    public void removeUrl(URL url){
        log.debug("Removing url: [{}].", url);

        List<SchedulerMethodConfig> configs = schedulerConfigs.remove(url);

        if(configs != null){
            for(SchedulerMethodConfig config : configs){
                mappedBeanConfigs.remove(config.beanString);
            }

            log.debug("Removed {} scheduled methods.", configs.size(), url);
        }
    }

    @Override
    public void stopScheduler() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[2];
        String beanName = element.getClassName() + "#" + element.getMethodName();
        SchedulerMethodConfig config = mappedBeanConfigs.remove(beanName);

        if(config == null){
            throw new IllegalStateException("No scheduler for bean with name: [" + beanName + "] found!");
        }

        for(URL url : schedulerConfigs.keySet()) {
            List<SchedulerMethodConfig> configs = schedulerConfigs.get(url);
            if(configs.remove(config)) break;
        }
    }

    @Override
    public String toString() {
        return "Scheduler";
    }
}
