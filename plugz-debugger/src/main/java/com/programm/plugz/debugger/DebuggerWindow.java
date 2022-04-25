package com.programm.plugz.debugger;

import com.programm.plugz.api.IAsyncManager;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class DebuggerWindow extends WindowAdapter {

    private static final String WIN_TITLE = "Plugz Debugger";
    private static final int WIN_INIT_WIDTH = 800;
    private static final int WIN_INIT_HEIGHT = 500;

    private final IAsyncManager asyncManager;
    private final JFrame frame = new JFrame();

    private final List<IDebugTab> tabs = new ArrayList<>();
    final TabValues tabValues;
    int tabIndex = 0;

    public DebuggerWindow(IAsyncManager asyncManager) {
        this.asyncManager = asyncManager;
        this.tabValues = new TabValues();

        tabs.add(tabValues);
        tabs.add(new TabThreadInfos());
        init();
    }

    public void init() {
        frame.setTitle(WIN_TITLE);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.setSize(WIN_INIT_WIDTH, WIN_INIT_HEIGHT);
        frame.setLocationRelativeTo(null);

        JTabbedPane mainPane = new JTabbedPane();
        mainPane.addChangeListener(e -> tabIndex = mainPane.getSelectedIndex());

        for(IDebugTab tab : tabs){
            mainPane.addTab(tab.name(), tab.view());
        }

        frame.add(mainPane);
    }

    public void setVisible(boolean visible){
        frame.setVisible(visible);
    }

    public void dispose(){
        frame.dispose();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        dispose();
        asyncManager.notifyCurrentThreadClose();
    }

    public IDebugTab getCurrentTab(){
        return tabs.get(tabIndex);
    }

}
