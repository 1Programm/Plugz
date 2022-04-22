package com.programm.plugz.debugger;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

class MagicDebugValueUI extends JPanel {

    final MagicDebugValue debugValue;
    private final JCheckBox enabledCheckbox;
    private final JLabel valueLabel;

    public MagicDebugValueUI(MagicDebugValue debugValue) {
        this.debugValue = debugValue;
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        this.enabledCheckbox = new JCheckBox();
        this.enabledCheckbox.setSelected(true);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.ipadx = 20;
        this.add(enabledCheckbox, gbc);

        JLabel nameLabel = new JLabel();
        nameLabel.setText(debugValue.name);
        nameLabel.setPreferredSize(new Dimension(200, 18));
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.ipadx = 0;
        this.add(nameLabel, gbc);

        this.valueLabel = new JLabel();
        this.valueLabel.setText("-");
        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.ipadx = 0;
        this.add(valueLabel, gbc);
    }

    public boolean enabled(){
        return enabledCheckbox.isSelected();
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
}
