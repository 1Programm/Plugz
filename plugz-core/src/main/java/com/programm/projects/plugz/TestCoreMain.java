package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.IOutput;

class TestCoreMain {

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

        Plugz plugz = Plugz.create()
                .addScanPath("file:/Users/julian/Desktop/Programming/Java/tests/PlugzTest/target/classes/")
                .build();
        plugz.scan();
        System.out.println(plugz);
    }

}
