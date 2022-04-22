package com.programm.plugz.debugger;


import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

class UIInitErrorPrintStreamFilter extends PrintStream {

    private static final Set<String> IGNORED_ERRORS_SET = new HashSet<>();
    static {
        IGNORED_ERRORS_SET.add("Warning: the fonts \"Times\" and \"Times\" are not available for the Java logical font \"Serif\", which may have unexpected appearance or behavior. Re-enable the \"Times\" font to remove this warning.");
    }

    public UIInitErrorPrintStreamFilter(OutputStream out) {
        super(out);
    }

    @Override
    public void println(String x) {
        if(IGNORED_ERRORS_SET.contains(x)){
            return;
        }

        super.println(x);
    }
}
