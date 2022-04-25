package com.programm.plugz.magic;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.api.IAsyncManager;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Logger("Thread-Pool-Manager")
class ThreadPoolManager implements IAsyncManager {

    private static final String CONF_MAX_WORKERS_NAME = "async.workers.max";
    private static final int CONF_MAX_WORKERS_DEFAULT = 5;

    private static final String CONF_TIMEOUT_NAME = "async.workers.timeout";
    private static final int CONF_TIMEOUT_DEFAULT = 2000;

    private static class TaskInfo {
        private static final int DEFAULT_INIT_CASHED_TASKS = 4;
        private static final Queue<TaskInfo> CACHED_TASKS = new ArrayDeque<>(DEFAULT_INIT_CASHED_TASKS);
        static {
            for(int i=0;i<DEFAULT_INIT_CASHED_TASKS;i++){ CACHED_TASKS.add(new TaskInfo()); }
        }

        public static TaskInfo create(Runnable task, Runnable onTaskFinished, long delay, boolean weakThread){
            TaskInfo nTask = CACHED_TASKS.poll();
            if(nTask == null){
                nTask = new TaskInfo();
            }

            nTask.set(task, onTaskFinished, delay, weakThread);
            return nTask;
        }

        private Runnable task;
        private Runnable onTaskFinished;
        private long delay;
        private boolean weakThread;

        public void set(Runnable task, Runnable onTaskFinished, long delay, boolean weakThread) {
            this.task = task;
            this.onTaskFinished = onTaskFinished;
            this.delay = delay;
            this.weakThread = weakThread;
        }

        public void free(){
            CACHED_TASKS.add(this);
        }
    }

    @RequiredArgsConstructor
    private class Worker {
        private final String name;
        private final boolean vip;
        private Thread thread;
        private TaskInfo taskInfo;
        private boolean running;

        public void start(TaskInfo taskInfo){
            this.taskInfo = taskInfo;
            thread = new Thread(this::run, name);
            thread.start();
        }

        private void run(){
            running = true;

            try {
                while (running && !Thread.interrupted()) {
                    if(taskInfo.delay > 0) Thread.sleep(taskInfo.delay);
                    taskInfo.task.run();
                    if(taskInfo.onTaskFinished != null) taskInfo.onTaskFinished.run();
                    taskInfo.free();

                    if(!running) break;

                    if(vip){
                        sleepingVipWorkers.add(this);
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
            catch (InterruptedException e){
                running = false;
            }

            log.trace("[{}]: no tasks available. Waiting for restart ...", name);
            notifyThreadClose(name);
        }

        private boolean checkQueue() throws InterruptedException{
            TaskInfo taskInfo = queuedTasks.poll(timeoutTime, TimeUnit.MILLISECONDS);

            if(taskInfo != null){
                this.taskInfo = taskInfo;
                return true;
            }

            return false;
        }
    }

    private Worker[] workers;
    private final Queue<Worker> sleepingWorkers = new ArrayDeque<>();

    private final List<Worker> vipWorkers = new ArrayList<>(2);
    private final Queue<Worker> sleepingVipWorkers = new ArrayDeque<>();

    private final BlockingQueue<TaskInfo> queuedTasks;

    private final ILogger log;
    private final List<String> accountableRunningThreadNames = new ArrayList<>();

    private boolean initialized;
    private int maxWorkers;
    private long timeoutTime;
    private boolean exited;
    private boolean enteredAutoClosableState;

    public ThreadPoolManager(ILogger log) {
        this.log = log;
        this.queuedTasks = new LinkedBlockingDeque<>();
    }

    public void init(ConfigurationManager configurations){
        this.maxWorkers = configurations.getIntOrRegisterDefault(CONF_MAX_WORKERS_NAME, CONF_MAX_WORKERS_DEFAULT);
        this.timeoutTime = configurations.getLongOrRegisterDefault(CONF_TIMEOUT_NAME, CONF_TIMEOUT_DEFAULT);
        this.workers = new Worker[maxWorkers];

        initialized = true;
    }

    public void shutdown(){
        this.exited = true;

        for(Worker worker : vipWorkers){
            if(worker.running) {
                shutdownWorker(worker);
            }
        }

        for(Worker worker : workers){
            if(worker == null) break;
            if(worker.running) {
                shutdownWorker(worker);
            }
        }
    }

    @Override
    public void runAsyncTask(Runnable task, Runnable onTaskFinished, long delay, boolean vip, boolean weakThread) {
        if(!initialized) throw new IllegalStateException("Cannot run tasks before initialisation!");
        if(exited) return;

        log.debug("Running async {}task: [{}].", (vip ? "VIP " : ""), task);

        TaskInfo taskInfo = TaskInfo.create(task, onTaskFinished, delay, weakThread);

        if(sleepingWorkers.isEmpty()){
            for(int i=0;i<maxWorkers;i++) {
                if(workers[i] == null){
                    String workerName = "Worker-" + i;
                    log.debug("Creating new Worker [{}].", workerName);
                    Worker worker = new Worker(workerName, false);
                    workers[i] = worker;
                    worker.start(taskInfo);
                    return;
                }
            }

            if(vip){
                if(sleepingVipWorkers.isEmpty()){
                    log.debug("No worker currently available. Creating new Vip Worker.");
                    Worker vipWorker = new Worker("VIP-Worker-" + vipWorkers.size(), true);
                    vipWorkers.add(vipWorker);
                    vipWorker.start(taskInfo);
                }
                else {
                    Worker vipWorker = pollSleepingVIPWorker();
                    log.debug("No worker currently available. Using [{}].", vipWorker.name);
                    vipWorker.start(taskInfo);
                }
            }
            else {
                log.debug("There are no workers available. Enqueueing task as the {}th task.", queuedTasks.size() + 1);
                queuedTasks.add(taskInfo);
            }
        }
        else {
            Worker worker = pollSleepingWorker();

            if(worker.running) {
                log.debug("Enqueueing task as there is at least 1 worker waiting for a new task", worker.name);
                queuedTasks.add(taskInfo);
            }
            else {
                log.debug("Restarting [{}].", worker.name);
                worker.start(taskInfo);
            }
        }
    }

    private void shutdownWorker(Worker worker){
        log.trace("Shutting down {}worker [{}]...", (worker.vip ? "VIP " : ""), worker.name);
        worker.running = false;
        worker.thread.interrupt();
    }

    private synchronized Worker pollSleepingWorker(){
        return sleepingWorkers.poll();
    }

    private synchronized Worker pollSleepingVIPWorker(){
        return sleepingVipWorkers.poll();
    }

    @Override
    public void notifyNewThread(String threadName) {
        log.debug("Adding thread name [{}] to the list for the auto-close system.", threadName);
        accountableRunningThreadNames.add(threadName);
    }

    @Override
    public synchronized void notifyThreadClose(String threadName) {
        if(enteredAutoClosableState) return;

        log.debug("Thread [{}] closed. Will try to start the auto-close mechanism...", threadName);

        for(int i=0;i<accountableRunningThreadNames.size();i++){
            String name = accountableRunningThreadNames.get(i);
            if(name.equals(threadName)){
                log.trace("Removing thread name [{}] from the list of the auto-close system.", threadName);
                accountableRunningThreadNames.remove(i);
                break;
            }
        }

        int estimateCount = Thread.activeCount();
        Thread[] allThreads = new Thread[estimateCount];
        Thread.enumerate(allThreads);

        for(Thread thread : allThreads){
            String name = thread.getName();
            if(accountableRunningThreadNames.contains(name)){
                log.trace("Accountable thread [{}] still running. So auto-close will cancel.", name);
                return;
            }
        }

        int runningNonWeakWorkers = 0;
        int runningWeakWorkers = 0;
        for(Worker w : vipWorkers){
            if(w.running){
                if(w.taskInfo.weakThread) {
                    runningWeakWorkers++;
                }
                else {
                    runningNonWeakWorkers++;
                }
            }
        }

        for(Worker w : workers){
            if(w == null) break;
            if(w.running){
                if(w.taskInfo.weakThread) {
                    runningWeakWorkers++;
                }
                else {
                    runningNonWeakWorkers++;
                }
            }
        }

        if(runningNonWeakWorkers == 0){
            enteredAutoClosableState = true;
            log.debug("Auto-close system detected closable state. [{}] weak workers will be terminated early.", runningWeakWorkers);

            for(Worker w : vipWorkers){
                if(w.running && w.taskInfo.weakThread){
                    shutdownWorker(w);
                }
            }

            for(Worker w : workers){
                if(w == null) break;
                if(w.running && w.taskInfo.weakThread){
                    shutdownWorker(w);
                }
            }
        }
    }
}
