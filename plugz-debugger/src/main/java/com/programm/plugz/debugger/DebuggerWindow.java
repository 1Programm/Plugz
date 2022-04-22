package com.programm.plugz.debugger;

import com.programm.plugz.api.IAsyncManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DebuggerWindow extends WindowAdapter {

    private static final String WIN_TITLE = "Plugz Debugger";
    private static final int WIN_INIT_WIDTH = 600;
    private static final int WIN_INIT_HEIGHT = 500;

    private final IAsyncManager asyncManager;
    private final JFrame frame = new JFrame();
    private final JPanel listPane;
    private final List<MagicDebugValueUI> debugValues = new ArrayList<>();

    public DebuggerWindow(IAsyncManager asyncManager) {
        this.asyncManager = asyncManager;
        listPane = new JPanel(new GridBagLayout());
        init();
    }

    public void init() {
        frame.setTitle(WIN_TITLE);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.setSize(WIN_INIT_WIDTH, WIN_INIT_HEIGHT);
        frame.setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        listPane.add(new JPanel(), gbc);

        frame.add(new JScrollPane(listPane));
    }

    public void setVisible(boolean visible){
        frame.setVisible(visible);
    }

    public void dispose(){
        frame.dispose();
    }

    public void addDebugValue(MagicDebugValue value){
        MagicDebugValueUI ui = new MagicDebugValueUI(value);

        if(value.dValueInstance != null){
            value.dValueInstance.addChangeListener(ui.getChangeListener());
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        listPane.add(ui, gbc, debugValues.size());
        debugValues.add(ui);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        dispose();
        asyncManager.notifyCurrentThreadClose();
    }

    public boolean hasFPSValues(){
        for(MagicDebugValueUI uiValue : debugValues){
            if(uiValue.debugValue.dValueInstance != null) continue;
            if(uiValue.enabled()) return true;
        }

        return false;
    }

    public void updateFPSValues(){
        for(MagicDebugValueUI uiValue : debugValues){
            if(uiValue.debugValue.dValueInstance != null || !uiValue.enabled()) continue;

            String value = getValueFromMagicDebugValue(uiValue.debugValue);
            uiValue.setValue(value);
        }
    }

    private String getValueFromMagicDebugValue(MagicDebugValue magicValue){
        try {
            if(magicValue.needsAccess) magicValue.field.setAccessible(true);
            String value =  Objects.toString(magicValue.field.get(magicValue.instance));
            if(magicValue.needsAccess) magicValue.field.setAccessible(false);
            return value;
        }
        catch (Exception e){
            return e.getMessage();
        }
    }
}
