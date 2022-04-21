package com.programm.plugz.magic;

import com.programm.plugz.api.utils.StringUtils;
import com.programm.projects.ioutils.log.api.out.*;

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
    public LoggerFallback format(String format) throws LoggerConfigException {
        return this;
    }

    @Override
    public LoggerFallback packageLevel(String pkg, int level) throws LoggerConfigException {
        return this;
    }

    @Override
    public IConfigurableLogger output(IOutput out) throws LoggerConfigException {
        this.out = out;
        return this;
    }
}
