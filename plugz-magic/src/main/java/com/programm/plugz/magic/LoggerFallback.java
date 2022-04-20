package com.programm.plugz.magic;

import com.programm.plugz.api.utils.StringUtils;
import com.programm.projects.ioutils.log.api.out.IConfigurableLogger;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.LevelLogger;
import com.programm.projects.ioutils.log.api.out.LoggerConfigException;

class LoggerFallback extends LevelLogger implements IConfigurableLogger {

    @Override
    protected void log(String msg, int level, Object... args) {
        if(level() > level) return;
        String _level = ILogger.levelToString(level);

        String msgWithArgs = StringUtils.format(msg, "{", "}", args);
        System.out.println("[" + _level + " ".repeat(5 - _level.length()) + "] " + msgWithArgs);
    }

    public LoggerFallback level(int level) throws LoggerConfigException {
        super.level(level);
        return this;
    }


    @Override
    public LoggerFallback format(String s) throws LoggerConfigException {
        return this;
    }

    @Override
    public LoggerFallback packageLevel(String s, int i) throws LoggerConfigException {
        return this;
    }
}
