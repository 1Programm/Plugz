package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.magic.api.ISchedules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ScheduleManager implements ISchedules {

    private final Thread schedulerThread = new Thread(this::run, "Scheduler#1");
    private final List<SchedulerMethodConfig> schedulerConfigs = new ArrayList<>();
    private final Map<String, SchedulerMethodConfig> mappedBeanConfigs = new HashMap<>();

    private boolean running;
    private boolean paused;

    private void run(){
        long now = curTime();
        for(SchedulerMethodConfig config : schedulerConfigs){
            config.startedAt = now;
        }

        while(running && !paused){
            try {
                Thread.sleep(100);

                now = curTime();
                for(int i=0;i<schedulerConfigs.size();i++){
                    try {
                        SchedulerMethodConfig config = schedulerConfigs.get(i);
                        if (!config.started) {
                            if (config.startedAt + config.startAfter < now) {
                                config.started = true;
                                config.startedAt = now;
                                config.startedLast = now;
                                config.run();
                                if (config.repeatAfter <= 0) {
                                    schedulerConfigs.remove(i);
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
                                schedulerConfigs.remove(i);
                                i--;
                            }
                        }
                    }
                    catch (MagicInstanceException e){
                        e.printStackTrace();
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

    public void scheduleRunnable(SchedulerMethodConfig config){
        schedulerConfigs.add(config);
        mappedBeanConfigs.put(config.beanString, config);

        if(running && paused) {
            paused = false;
            schedulerThread.start();
        }


        if(running){
            config.startedAt = curTime();
        }
    }

    @Override
    public void stopScheduler() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[2];
        String beanName = element.getClassName() + "#" + element.getMethodName();
        SchedulerMethodConfig config = mappedBeanConfigs.get(beanName);
        schedulerConfigs.remove(config);
    }
}
