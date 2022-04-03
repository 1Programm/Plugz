package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.IAsyncManager;
import com.programm.projects.plugz.magic.api.SysArgs;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Logger("Thread-Pool-Manager")
class ThreadPoolManager implements IAsyncManager {

    @RequiredArgsConstructor
    private class Worker {

        private final String name;
        private Thread thread;
        private Runnable task;
        private boolean running;
        private boolean vip;
        private long delay;

        public void start(Runnable task, long delay){
            this.task = task;
            this.delay = delay;
            thread = new Thread(this::run, name);
            thread.start();
        }

        private void run(){
            running = true;

            try {
                while (running && !Thread.interrupted()) {
                    Thread.sleep(delay);
                    task.run();

                    if(vip){
                        vipWorkers.remove(this);
                        return;
                    }

                    sleepingWorkers.add(this);

                    if (!checkQueue()) {
                        running = false;
                    }
                    else {
                        sleepingWorkers.remove(this);
                    }
                }
            }
            catch (InterruptedException ignore){}

            log.debug("[{}]: no tasks available. Waiting for restart ...", name);
        }

        private boolean checkQueue() throws InterruptedException{
            Runnable task = queuedTasks.poll(sleepTime, TimeUnit.MILLISECONDS);

            if(task != null){
                this.task = task;
                return true;
            }

            return false;
        }
    }

    private static final int DEFAULT_MAX_WORKERS = 5;
    private static final long DEFAULT_MAX_WORKER_SLEEP_TIME = 5000;

    private final List<Worker> sleepingWorkers = new ArrayList<>();
    private final List<Worker> vipWorkers = new ArrayList<>();
    private final BlockingQueue<Runnable> queuedTasks;

    private final ILogger log;
    private final int maxWorkers;
    private final long sleepTime;
    private final boolean[] workerExists;

    public ThreadPoolManager(ILogger log, SysArgs args) {
        this.log = log;
        this.maxWorkers = args.getDefault("-maxAsyncWorkers", DEFAULT_MAX_WORKERS);
        this.sleepTime = args.getDefault("-maxWorkerSleep", DEFAULT_MAX_WORKER_SLEEP_TIME);
        this.workerExists = new boolean[maxWorkers];
        this.queuedTasks = new LinkedBlockingDeque<>();
    }

    @Override
    public void runAsyncVipTask(Runnable task, long delay){
        log.trace("Running async VIP task: [{}].", task);

        if(sleepingWorkers.isEmpty()){
            for(int i=0;i<maxWorkers;i++) {
                if(!workerExists[i]){
                    workerExists[i] = true;
                    String workerName = "Worker-" + i;
                    log.debug("Creating new Worker [{}].", workerName);
                    Worker worker = new Worker(workerName);
                    worker.start(task, delay);
                    return;
                }
            }

            log.debug("No worker currently available. New Vip Worker will be created.");
            Worker worker = new Worker("VIP-Worker-" + vipWorkers.size());
            worker.vip = true;
            vipWorkers.add(worker);
            worker.start(task, delay);
        }
        else {
            Worker worker = sleepingWorkers.remove(0);

            if(worker.running) {
                log.debug("Waking up [{}].", worker.name);
                worker.delay = delay;
                queuedTasks.add(task);
            }
            else {
                log.debug("Restarting [{}].", worker.name);
                worker.start(task, delay);
            }
        }
    }

    @Override
    public void runAsyncTask(Runnable task, long delay){
        log.trace("Running async task: [{}].", task);

        if(sleepingWorkers.isEmpty()){
            for(int i=0;i<maxWorkers;i++) {
                if(!workerExists[i]){
                    workerExists[i] = true;
                    String workerName = "Worker-" + i;
                    log.debug("Creating new Worker [{}].", workerName);
                    Worker worker = new Worker(workerName);
                    worker.start(task, delay);
                    return;
                }
            }

            log.debug("No worker currently available. Enqueueing task as the {}th task.", queuedTasks.size());
            //TODO: Delay
            queuedTasks.add(task);
        }
        else {
            Worker worker = sleepingWorkers.remove(0);

            if(worker.running) {
                log.debug("Waking up [{}].", worker.name);
                worker.delay = delay;
                queuedTasks.add(task);
            }
            else {
                log.debug("Restarting [{}].", worker.name);
                worker.start(task, delay);
            }
        }
    }

    @Override
    public void shutdown(){
        //TODO: working workers are not listed and cannot be interrupted
        for(Worker worker : sleepingWorkers){
            if(worker.running) {
                log.trace("Shutting down sleeping worker [{}]...", worker.name);
                worker.running = false;
                worker.thread.interrupt();
            }
        }
    }

}
