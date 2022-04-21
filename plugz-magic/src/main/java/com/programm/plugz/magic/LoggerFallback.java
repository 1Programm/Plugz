package com.programm.plugz.magic;

import com.programm.ioutils.io.api.IOutput;
import com.programm.ioutils.log.api.IConfigurableLogger;
import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.LevelLogger;
import com.programm.ioutils.log.api.LoggerConfigException;
import com.programm.plugz.api.utils.StringUtils;

class LoggerFallback extends LevelLogger implements IConfigurableLogger {

    private IOutput out;

    @Override
    protected void log(String msg, int level, Object... args) {
        if(level() > level) return;
        String _level = ILogger.levelToString(level);

        String msgWithArgs = StringUtils.format(msg, "{", "}", args);
        out.println("[" + _level + " ".repeat(5 - _level.length()) + "] " + msgWithArgs);
    }

    public LoggerFallback level(int level) throws LoggerConfigException {
        super.level(level);
        return this;
    }

    @Override
    public void setNextLogInfo(Class<?> cls, String methodName) {}

    @Override
    public LoggerFallback format(String format) throws LoggerConfigException { return this; }

    @Override
    public LoggerFallback packageLevel(String pkg, int level) throws LoggerConfigException { return this; }

    @Override
    public IConfigurableLogger output(IOutput out) throws LoggerConfigException {
        this.out = out;
        return this;
    }
}
