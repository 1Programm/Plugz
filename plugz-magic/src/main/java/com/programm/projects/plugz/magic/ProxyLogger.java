package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;

/**
 * All Methods must be synchronized as the ThreadPoolManager and the async workers will log stuff and could end up overlapping logs.
 */
class ProxyLogger implements ILogger {

    private ILogger logger;

    @Override
    public synchronized void trace(String s, Object... objects) {
        if(logger == null) return;
        logger.trace(s, objects);
    }

    @Override
    public synchronized void debug(String s, Object... objects) {
        if(logger == null) return;
        logger.debug(s, objects);
    }

    @Override
    public synchronized void info(String s, Object... objects) {
        if(logger == null) return;
        logger.info(s, objects);
    }

    @Override
    public synchronized void warn(String s, Object... objects) {
        if(logger == null) return;
        logger.warn(s, objects);
    }

    @Override
    public synchronized void error(String s, Object... objects) {
        if(logger == null) return;
        logger.error(s, objects);
    }

    @Override
    public int level() {
        if(logger == null) return LEVEL_NONE;
        return logger.level();
    }

    public ILogger getLogger() {
        return logger;
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }
}
