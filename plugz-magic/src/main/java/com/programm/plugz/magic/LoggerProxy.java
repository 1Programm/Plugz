package com.programm.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;

import java.util.ArrayList;
import java.util.List;

/**
 * A proxy wrapper so this proxy can be used for auto - setting without the need to actually use a logger.
 * All Methods must be synchronized as the ThreadPoolManager and the async workers will log stuff and could end up overlapping logs.
 */
class LoggerProxy implements ILogger {

    private static class LogInfo {
        private final int level;
        private final String s;
        private final Object[] args;

        public LogInfo(int level, String s, Object[] args) {
            this.level = level;
            this.s = s;
            this.args = args;
        }
    }

    public ILogger logger;
    private final List<LogInfo> storedLogs = new ArrayList<>();

    public void passStoredLogs(){
        for(LogInfo info : storedLogs){
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

    private void doLog(int level, String s, Object... args){
        if(logger == null) {
            storedLogs.add(new LogInfo(level, s, args));
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
        if(logger == null) return LEVEL_NONE;
        return logger.level();
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }
}