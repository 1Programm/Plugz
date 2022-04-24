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
    private static final int WIN_INIT_WIDTH = 800;
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
        tryRegisterMagicValue(ui);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        listPane.add(ui, gbc, debugValues.size());
        debugValues.add(ui);
    }

    private void tryRegisterMagicValue(MagicDebugValueUI value){
        if(value.debugValue.dValueInstance != null){
            if(value.childrenUI.length == 0) {
                value.debugValue.dValueInstance.addChangeListener(value.getChangeListener());
            }
            else {
                for(MagicDebugValueUI childValue : value.childrenUI){
                    tryRegisterMagicValue(childValue);
                }
            }
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        dispose();
        asyncManager.notifyCurrentThreadClose();
    }

    public boolean hasFPSValues(){
        for(MagicDebugValueUI uiValue : debugValues){
            if(isFPSValue(uiValue)) return true;
        }

        return false;
    }

    private boolean isFPSValue(MagicDebugValueUI value){
        return value.debugValue.dValueInstance == null && value.enabled();
    }

    public void updateFPSValues(){
        for(MagicDebugValueUI uiValue : debugValues){
            updateFPSValue(uiValue);
        }
    }

    private void updateFPSValue(MagicDebugValueUI value){
        if(value.debugValue.children.length == 0) {
            if (value.debugValue.dValueInstance != null || !value.enabled()) return;

            if(value.debugValue.instance != null) {
                String _value = Objects.toString(getValueFromMagicDebugValue(value.debugValue));
                value.setValue(_value);
            }
        }
        else {
            if(value.enabled()){
                if(value.debugValue.dValueInstance == null && value.debugValue.instance != null && value.isChildrenVisible) {
                    Object _value = getValueFromMagicDebugValue(value.debugValue);
                    for (MagicDebugValueUI childValue : value.childrenUI) {
                        childValue.debugValue.instance = _value;
                    }
                }

                if(value.isChildrenVisible) {
                    for (MagicDebugValueUI childValue : value.childrenUI) {
                        updateFPSValue(childValue);
                    }
                }
            }
        }
    }

    private Object getValueFromMagicDebugValue(MagicDebugValue magicValue){
        try {
            if(magicValue.needsAccess) magicValue.field.setAccessible(true);
            Object value = magicValue.field.get(magicValue.instance);
            if(magicValue.needsAccess) magicValue.field.setAccessible(false);
            return value;
        }
        catch (Exception e){
            return e.getMessage();
        }
    }
}
