package com.programm.plugz.debugger;

import javax.swing.*;

interface IDebugTab {

    String name();

    JComponent view();

    void update();

}
