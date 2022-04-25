package com.programm.plugz.debugger;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class TabThreadInfos implements IDebugTab {

    private final JPanel panel = new JPanel(new GridBagLayout());

    @Override
    public String name() {
        return "Threads";
    }

    @Override
    public JComponent view() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        update();

        return new JScrollPane(panel);
    }

    @Override
    public void update() {
        int estimateCount = Thread.activeCount();
        Thread[] allThreads = new Thread[estimateCount];
        Thread.enumerate(allThreads);

        panel.removeAll();
        for(Thread thread : allThreads){
            panel.add(new JLabel(thread.getName()));
        }

        panel.revalidate();
        panel.repaint();
    }
}
