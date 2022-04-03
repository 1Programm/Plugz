package com.programm.projects.plugz.simple.debug;

import com.programm.projects.plugz.magic.api.debug.IValue;

import javax.swing.*;

class DebugValueImpl <T> implements IValue<T> {

    final int id;
    final JPanel panel = new JPanel();
    final JLabel valueLabel = new JLabel();
    T value;

    public DebugValueImpl(int id, String name, T value) {
        this.id = id;
        this.value = value;
        this.panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        this.panel.add(new JLabel(name + ": "));
        this.valueLabel.setText(Utils.toString(value));
        this.panel.add(valueLabel);
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T o) {
        if(SimpleDebugger.instance != null) {
            SimpleDebugger.instance.valueChanged(id, o);
        }

        this.value = o;
    }

    @Override
    public String toString() {
        return Utils.toString(value);
    }
}
