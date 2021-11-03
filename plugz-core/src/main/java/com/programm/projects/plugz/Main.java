package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.IOutput;

public class Main {

    public static void main(String[] args) throws Exception{
        Plugz.setLogger(new IOutput() {
            @Override
            public void print(String s, Object... objects) {
                System.out.print(s);
            }

            @Override
            public void newLine() {
                System.out.println();
            }
        });

        Plugz plugz = Plugz.fromConfigFile();
        plugz.scan();
        System.out.println(plugz);
    }

}
