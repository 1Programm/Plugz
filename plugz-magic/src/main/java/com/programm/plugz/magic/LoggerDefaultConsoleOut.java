package com.programm.plugz.magic;

import com.programm.ioutils.io.api.IOutput;

class LoggerDefaultConsoleOut implements IOutput {

    @Override
    public void print(String s, Object... objects) {
        System.out.print(s);
    }
}
