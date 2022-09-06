package com.programm.plugz.magic;

import com.programm.ioutils.log.api.IConfigurableLogger;
import com.programm.ioutils.log.api.ILogger;

import java.util.ArrayList;
import java.util.List;

/**
 * hallo.A proxy wrapper so this proxy can be used for auto - setting without the need to actually use a logger.
 * All Methods must be synchronized as the ThreadPoolManager and the async workers will log stuff and could end up overlapping logs.
 */
class LoggerProxy implements ILogger {

    private static class LogInfo {
        private final int level;
        private final String s;
        private final Object[] args;
        private final Class<?> logInfoCallingCls;
        private final String logInfoCallingMethodName;

        private final String throwableMessage;
        private final Throwable throwable;

        public LogInfo(int level, String s, Object[] args, Class<?> logInfoCallingCls, String logInfoCallingMethodName, String throwableMessage, Throwable throwable) {
            this.level = level;
            this.s = s;
            this.args = args;
            this.logInfoCallingCls = logInfoCallingCls;
            this.logInfoCallingMethodName = logInfoCallingMethodName;
            this.throwableMessage = throwableMessage;
            this.throwable = throwable;
        }
    }

    public ILogger logger;
    private boolean storeLogs = true;
    private final List<LogInfo> storedLogs = new ArrayList<>();

    public void passStoredLogs(){
        if(logger == null) return;
        storeLogs = false;

        boolean isConfigLogger = logger instanceof IConfigurableLogger;
        for(LogInfo info : storedLogs){
            if(info.throwable != null){
                logException(info.throwableMessage, info.throwable);
                continue;
            }

            if (isConfigLogger && info.logInfoCallingCls != null) {
                ((IConfigurableLogger)logger).setNextLogInfo(info.logInfoCallingCls, info.logInfoCallingMethodName);
            }
            doLog(info.level, info.s, info.args);
        }

        storedLogs.clear();
    }

    @Override
    public synchronized void trace(String s, Object... args) {
        doLog(LEVEL_TRACE, s, args);
    }

    @Override
    public synchronized void debug(String s, Object... args) {
        doLog(LEVEL_DEBUG, s, args);
    }

    @Override
    public synchronized void info(String s, Object... args) {
        doLog(LEVEL_INFO, s, args);
    }

    @Override
    public synchronized void warn(String s, Object... args) {
        doLog(LEVEL_WARN, s, args);
    }

    @Override
    public synchronized void error(String s, Object... args) {
        doLog(LEVEL_ERROR, s, args);
    }

    @Override
    public void logException(String s, Throwable throwable) {
        if(storeLogs){
            storedLogs.add(new LogInfo(LEVEL_NONE, null, null, null, null, s, throwable));
        }
        else {
            logger.logException(s, throwable);
        }
    }

    private void doLog(int level, String s, Object... args){
        if(storeLogs) {
            storedLogs.add(getInfo(level, s, args));
        }
        else {
            switch (level){
                case(LEVEL_TRACE) -> logger.trace(s, args);
                case(LEVEL_DEBUG) -> logger.debug(s, args);
                case(LEVEL_INFO) -> logger.info(s, args);
                case(LEVEL_WARN) -> logger.warn(s, args);
                case(LEVEL_ERROR) -> logger.error(s, args);
            }
        }
    }

    @Override
    public int level() {
        if(logger == null) return LEVEL_TRACE;
        return logger.level();
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }

    private LogInfo getInfo(int level, String s, Object... args){
        Class<?> callingClass = null;
        String callingMethodName = null;

        StackTraceElement[] callers = Thread.currentThread().getStackTrace();

        for(int i=callers.length-2;i>2;i--) {
            StackTraceElement caller = callers[i];
            String curMethodName = caller.getMethodName();

            if (curMethodName.equals("trace") || curMethodName.equals("debug") || curMethodName.equals("info") || curMethodName.equals("warn") || curMethodName.equals("error")) {
                String fullClsName = callers[i + 1].getClassName();
                String methodName = callers[i + 1].getMethodName();
                try {
                    callingClass = LoggerProxy.class.getClassLoader().loadClass(fullClsName);
                    callingMethodName = methodName;
                    break;
                } catch (ClassNotFoundException ignore) {}
            }
        }

        if(callingClass == null) {
            throw new IllegalStateException("Could not find caller method of logger!");
        }

        return new LogInfo(level, s, args, callingClass, callingMethodName, null, null);
    }
}