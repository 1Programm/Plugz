package com.programm.plugz.debugger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class TabValues implements IDebugTab {

    private final JPanel listPane;
    private final List<MagicDebugValueUI> debugValues = new ArrayList<>();

    public TabValues(){
        listPane = new JPanel(new GridBagLayout());
    }

    @Override
    public String name() {
        return "Values";
    }

    @Override
    public JComponent view() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        listPane.add(new JPanel(), gbc);

        return new JScrollPane(listPane);
    }

    @Override
    public void update() {
        for(MagicDebugValueUI uiValue : debugValues){
            updateFPSValue(uiValue);
        }
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
                if(value.debugValue.dValueInstance == null && value.debugValue.instance != null) {
                    Object instance = getValueFromMagicDebugValue(value.debugValue);
                    value.setInstance(instance);
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
