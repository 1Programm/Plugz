package com.programm.plugz.magic;

import com.programm.ioutils.io.api.IOutput;
import com.programm.ioutils.log.api.*;
import com.programm.plugz.api.utils.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

class LoggerFallback extends LevelLogger implements IConfigurableLogger {

    private IOutput out;
    private boolean doPrintStacktrace;

    @Override
    protected void log(String msg, int level, Object... args) {
        if(level() > level) return;
        String _level = ILogger.levelToString(level);

        String msgWithArgs = StringUtils.format(msg, "{", "}", args);
        out.println("[" + _level + " ".repeat(5 - _level.length()) + "] " + msgWithArgs);
    }

    @Override
    public void logException(String msg, Throwable t) {
        if(msg == null) msg = t.getMessage();
        error(msg);

        if(doPrintStacktrace){
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            out.println(sw.toString());
        }
    }

    public LoggerFallback level(int level) throws LoggerConfigException {
        super.level(level);
        return this;
    }

    @Override
    public void setNextLogInfo(Class<?> cls, String methodName) {}

    @Override
    public LoggerFallback config(String s, Object... objects) {
        if(s.equals("level")){
            level((int) objects[0]);
        }
        else if(s.equals("output")){
            this.out = (IOutput) objects[0];
        }
        else if(s.equals("printStacktraceForExceptions")){
            this.doPrintStacktrace = (boolean) objects[0];
        }

        return this;
    }

}
