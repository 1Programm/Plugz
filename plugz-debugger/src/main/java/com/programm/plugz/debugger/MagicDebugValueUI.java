package com.programm.plugz.debugger;

import com.programm.plugz.api.utils.ValueParseException;
import com.programm.plugz.api.utils.ValueUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.Objects;

class MagicDebugValueUI extends JPanel {

    final MagicDebugValue debugValue;
    private final JButton collapseChildren;
    private final JCheckBox enabledCheckbox;
    private final JLabel valueLabel;
    private final JButton editSaveButton;
    private final JTextField valueInputField;

    final MagicDebugValueUI[] childrenUI;

    private final int numChildren;

    private final Border errBorder = BorderFactory.createLineBorder(Color.RED);

    boolean isChildrenVisible;
    private boolean isEditing;
    private boolean enabled = true;

    public MagicDebugValueUI(MagicDebugValue debugValue) {
        this.debugValue = debugValue;
        this.numChildren = debugValue.children.length;
        this.childrenUI = new MagicDebugValueUI[numChildren];

        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        if(numChildren != 0){
            this.collapseChildren = new JButton(">");
            this.collapseChildren.setPreferredSize(new Dimension(20, 18));
            this.collapseChildren.addActionListener(this::onCollapseChildren);
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.ipadx = 5;
            this.add(collapseChildren, gbc);

            Insets defaultInsets = gbc.insets;
            for(int i=0;i<numChildren;i++){
                childrenUI[i] = new MagicDebugValueUI(debugValue.children[i]);
                childrenUI[i].setVisible(false);
                gbc.insets = new Insets(0, 25, 0, 0);
                gbc.gridy = i + 1;
                gbc.gridwidth = 7;
                gbc.weightx = 0;
                this.add(childrenUI[i], gbc);
            }
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.insets = defaultInsets;
        }
        else {
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.ipadx = 5;
            this.add(Box.createHorizontalStrut(20), gbc);
            this.collapseChildren = null;
        }

        this.enabledCheckbox = new JCheckBox();
        this.enabledCheckbox.addActionListener(this::onEnabledToggle);
        this.enabledCheckbox.setSelected(true);
        gbc.weightx = 0;
        gbc.ipadx = 20;
        this.add(enabledCheckbox, gbc);

        JLabel typeLabel = new JLabel();
        typeLabel.setText(debugValue.field.getType().getSimpleName());
        typeLabel.setPreferredSize(new Dimension(100, 18));
        gbc.weightx = 0;
        gbc.ipadx = 0;
        this.add(typeLabel, gbc);


        JLabel nameLabel = new JLabel();
        nameLabel.setText(debugValue.name);
        nameLabel.setPreferredSize(new Dimension(200, 18));
        gbc.weightx = 0;
        gbc.ipadx = 0;
        this.add(nameLabel, gbc);

        this.valueLabel = new JLabel();
        if(numChildren == 0) this.valueLabel.setText("-");
        gbc.weightx = 1;
        gbc.ipadx = 0;
        this.add(valueLabel, gbc);

        this.editSaveButton = new JButton("...");
        if(numChildren == 0) {
            this.editSaveButton.addActionListener(this::onEditSave);
        }
        else {
            this.editSaveButton.setEnabled(false);
        }
        gbc.weightx = 0;
        gbc.ipadx = 0;
        this.add(editSaveButton, gbc);

        this.valueInputField = new JTextField();
        this.valueInputField.setPreferredSize(new Dimension(200, 18));
        this.valueInputField.setEnabled(false);
        gbc.weightx = 0;
        gbc.ipadx = 0;
        this.add(valueInputField, gbc);
    }

    public boolean enabled(){
        return enabled;
    }

    public void setValue(String value){
        this.valueLabel.setText(value);
    }

    public Runnable getChangeListener(){
        return () -> {
            if(!enabled()) return;

            SwingUtilities.invokeLater(() -> {
                String val = Objects.toString(debugValue.dValueInstance.get());
                setValue(val);
            });
        };
    }

    private void onCollapseChildren(ActionEvent e){
        if(isChildrenVisible){
            this.collapseChildren.setText(">");
            for(int i=0;i<numChildren;i++){
                childrenUI[i].setVisible(false);
            }
        }
        else {
            this.collapseChildren.setText("V");
            for(int i=0;i<numChildren;i++){
                childrenUI[i].setVisible(true);
            }
        }

        isChildrenVisible = !isChildrenVisible;
    }

    private void onEnabledToggle(ActionEvent e){
        enabled = enabledCheckbox.isSelected();

        if(enabled) {
            this.setBackground(null);
            this.editSaveButton.setEnabled(true);
        }
        else {
            this.setBackground(Color.LIGHT_GRAY);
            this.editSaveButton.setText("...");
            this.editSaveButton.setEnabled(false);
            this.valueInputField.setEnabled(false);
            this.valueInputField.setText("");
        }

        for(int i=0;i<childrenUI.length;i++){
            childrenUI[i].setValueEnabled(enabled);
        }
    }

    private void setValueEnabled(boolean enabled){
        boolean isEnabled = enabled && this.enabled;

        if(isEnabled) {
            this.setBackground(null);
            this.editSaveButton.setEnabled(true);
        }
        else {
            this.setBackground(Color.LIGHT_GRAY);
            this.editSaveButton.setText("...");
            this.editSaveButton.setEnabled(false);
            this.valueInputField.setEnabled(false);
            this.valueInputField.setText("");
        }

        for(int i=0;i<childrenUI.length;i++){
            childrenUI[i].setValueEnabled(isEnabled);
        }
    }

    private void onEditSave(ActionEvent e){
        if(isEditing){
            String value = valueInputField.getText();
            if(saveValue(value)) {
                valueInputField.setBorder(null);
            }
            else {
                valueInputField.setBorder(errBorder);
                return;
            }

            editSaveButton.setText("...");
            valueInputField.setEnabled(false);
            valueInputField.setText("");
        }
        else {
            editSaveButton.setText("Save");
            valueInputField.setEnabled(true);
        }
        isEditing = !isEditing;
    }

    private boolean saveValue(String _value){
        Class<?> type;

        if(debugValue.dValueInstance != null){
            type = debugValue.dValueInstance.type;
        }
        else {
            type = debugValue.field.getType();
        }

        try {
            Object value = ValueUtils.parsePrimitive(_value, type);
            if(debugValue.dValueInstance != null){
                debugValue.dValueInstance.setValue(value);
            }
            else {
                setDebugField(value);
            }

            return true;
        }
        catch (ValueParseException e){
            System.err.println(e.getMessage());
            return false;
        }
    }

    private void setDebugField(Object value){
        Field f = debugValue.field;

        if (debugValue.needsAccess) f.setAccessible(true);
        try {
            f.set(debugValue.instance, value);
        }
        catch (IllegalAccessException e){
            e.printStackTrace();
        }
        finally {
            if (debugValue.needsAccess) f.setAccessible(false);
        }
    }
}
