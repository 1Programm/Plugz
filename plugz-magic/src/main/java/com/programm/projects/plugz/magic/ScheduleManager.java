package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.magic.api.ISchedules;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ScheduleManager implements ISchedules {

    private final Thread schedulerThread = new Thread(this::run, "Scheduler#1");
    private final Map<URL, List<SchedulerMethodConfig>> schedulerConfigs = new HashMap<>();
    private final Map<String, SchedulerMethodConfig> mappedBeanConfigs = new HashMap<>();

    private boolean running;
    private boolean paused;

    private void run(){
        long now = curTime();
        for(URL url : schedulerConfigs.keySet()) {
            for (SchedulerMethodConfig config : schedulerConfigs.get(url)) {
                config.startedAt = now;
            }
        }

        while(running && !paused){
            try {
                Thread.sleep(100);

                now = curTime();
                for(URL url : schedulerConfigs.keySet()) {
                    List<SchedulerMethodConfig> configs = schedulerConfigs.get(url);

                    for (int i = 0; i < configs.size(); i++) {
                        try {
                            SchedulerMethodConfig config = configs.get(i);
                            if (!config.started) {
                                if (config.startedAt + config.startAfter < now) {
                                    config.started = true;
                                    config.startedAt = now;
                                    config.startedLast = now;
                                    config.run();
                                    if (config.repeatAfter <= 0) {
                                        configs.remove(i);
                                        i--;
                                    }
                                }
                            } else {
                                if (config.startedLast + config.repeatAfter < now) {
                                    config.startedLast = now;
                                    config.run();
                                }
                            }

                            if (config.stopAfter != 0) {
                                if (config.startedAt + config.stopAfter < now) {
                                    configs.remove(i);
                                    i--;
                                }
                            }
                        } catch (MagicInstanceException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (InterruptedException ignore){}

            if(schedulerConfigs.size() == 0){
                paused = true;
            }
        }
    }

    private long curTime(){
        return System.currentTimeMillis();
    }

    public void startup(){
        if(running) return;
        running = true;
        paused = false;

        schedulerThread.start();
    }

    public void shutdown(){
        running = false;
        schedulerThread.interrupt();
    }

    public void scheduleRunnable(URL fromUrl, SchedulerMethodConfig config){
        schedulerConfigs.computeIfAbsent(fromUrl, url -> new ArrayList<>()).add(config);
        mappedBeanConfigs.put(config.beanString, config);

        if(running && paused) {
            paused = false;
            schedulerThread.start();
        }


        if(running){
            config.startedAt = curTime();
        }
    }

    public void removeUrl(URL url){
        List<SchedulerMethodConfig> configs = schedulerConfigs.remove(url);

        if(configs != null){
            for(SchedulerMethodConfig config : configs){
                mappedBeanConfigs.remove(config.beanString);
            }
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
}
