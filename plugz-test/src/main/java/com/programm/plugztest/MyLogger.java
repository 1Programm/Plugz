package com.programm.plugztest;

import com.programm.plugz.api.utils.StringUtils;
import com.programm.projects.ioutils.log.api.out.LevelLogger;

public class MyLogger extends LevelLogger {

    @Override
    protected void log(String s, int i, Object... objects) {
        System.out.println(StringUtils.format(s, "{", "}", objects));
    }
}
