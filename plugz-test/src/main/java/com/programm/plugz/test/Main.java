package com.programm.plugz.test;

import com.programm.plugz.magic.MagicEnvironment;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.IOutput;
import com.programm.projects.ioutils.log.logger.ConfLogger;

public class Main {

    private static class ThreadNamePrependOut implements IOutput {
        @Override
        public void print(String s, Object... objects) {
            System.out.print(s);
        }

        @Override
        public void println(String message, Object... args) {
            int num = 20;
            String threadName = Thread.currentThread().getName();
            message = "[" + threadName + " ".repeat(Math.max(0, num - threadName.length())) + "]: " + message;
            IOutput.super.println(message, args);
        }
    }

    public static void main(String[] args) throws Exception {
        MagicEnvironment env = new MagicEnvironment(args);
        env.setLogger(new ConfLogger("[%5<($LVL)] [%30>($LOG?{$CLS.$MET})]: $MSG", ILogger.LEVEL_INFO, new ThreadNamePrependOut()));
        env.setup();
        env.startup();
    }

}
