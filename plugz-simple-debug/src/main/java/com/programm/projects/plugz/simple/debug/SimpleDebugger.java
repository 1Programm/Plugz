package com.programm.projects.plugz.simple.debug;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.MagicException;
import com.programm.projects.plugz.magic.api.debug.Debug;
import com.programm.projects.plugz.magic.api.debug.IDebugManager;
import com.programm.projects.plugz.magic.api.debug.IValue;
import com.programm.projects.plugz.magic.api.debug.MagicDebugSetupException;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Semaphore;

@Logger("Simple-Debugger")
public class SimpleDebugger implements IDebugManager {

    static SimpleDebugger instance;

    private final JFrame frame = new JFrame("Debugger");
    private final JPanel panel = new JPanel();

    private final Map<Integer, DebugValueImpl<?>> valueMap = new HashMap<>();

    @Get private ILogger log;
    private int idCounter = 0;

    public SimpleDebugger() {
        if(instance != null) throw new IllegalStateException("Instance of debugger already exists!");
        instance = this;
        initUi();
    }

    private void initUi(){
        Dimension dim = new Dimension(600, 600);
        frame.setMinimumSize(dim);
        frame.setMaximumSize(dim);
        frame.setSize(dim);

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setResizable(false);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Variables:");
        panel.add(label);

        frame.add(new JScrollPane(panel));
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    @Override
    public void startup() throws MagicException {
        log.info("Start");
        frame.setVisible(true);
    }

    @Override
    public void shutdown() throws MagicException {
        log.info("Stop");
        frame.dispose();
    }

    @Override
    public void registerDebugValue(Object instance, Field field, Debug debugAnnotation) throws MagicDebugSetupException {
        log.info("Register Value: [{}]", field);

        boolean isPrivate = false;

        int mods = field.getModifiers();
        if(Modifier.isFinal(mods)) throw new MagicDebugSetupException("Debug - field [" + field + "] must not be final!");
        if(Modifier.isPrivate(mods)) isPrivate = true;

        if(field.getType() != IValue.class) throw new MagicDebugSetupException("Debug - field [" + field + "] must be of value " + IValue.class + "!");

        Type fieldType = field.getGenericType();
        ParameterizedType paramFieldType = (ParameterizedType)fieldType;
        Type contentType = paramFieldType.getActualTypeArguments()[0];

        Class<?> contentCls;

        try {
            contentCls = Class.forName(contentType.getTypeName());
        }
        catch (ClassNotFoundException e){
            throw new MagicDebugSetupException("Failed to get the content type class for debug-field [" + field + "]!", e);
        }

        contentCls = toPrimitive(contentCls);

        Object defaultValue;

        if(contentCls.isPrimitive()){
            defaultValue = getPrimitiveDefault(contentCls);
        }
        else if(contentCls == String.class){
            defaultValue = "";
        }
        else {
            defaultValue = null;
        }

        String name = getName(field, debugAnnotation);

        DebugValueImpl<?> val = newVal(name, defaultValue);
        valueMap.put(val.id, val);

        try {
            if (isPrivate) field.setAccessible(true);
            field.set(instance, val);
            if (isPrivate) field.setAccessible(false);
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE", e);
        }

        panel.add(val.panel);
        frame.revalidate();
    }

    private String getName(Field field, Debug debugAnnotation){
        String name = debugAnnotation.value();
        if(name.equals("")) name = field.getName();

        return name;
    }

    private DebugValueImpl<?> newVal(String name, Object defaultValue){
        return new DebugValueImpl<>(idCounter++, name, defaultValue);
    }

    private Class<?> toPrimitive(Class<?> cls){
        try {
            return (Class<?>) cls.getField("TYPE").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return cls;
        }
    }

    private Object getPrimitiveDefault(Class<?> cls){
        if(cls == Boolean.TYPE){
            return false;
        }
        else if(cls == Character.TYPE){
            return 0;
        }
        else if(cls == Byte.TYPE){
            return (byte)0;
        }
        else if(cls == Short.TYPE){
            return (short)0;
        }
        else if(cls == Integer.TYPE){
            return 0;
        }
        else if(cls == Long.TYPE){
            return 0L;
        }
        else if(cls == Float.TYPE){
            return 0f;
        }
        else if(cls == Double.TYPE){
            return 0d;
        }

        throw new IllegalStateException("INVALID STATE: " + cls);
    }

    void valueChanged(int id, Object value){
        DebugValueImpl<?> val = valueMap.get(id);

        if(val == null) return;

        SwingUtilities.invokeLater(() -> {
            val.valueLabel.setText(Utils.toString(value));
            //frame.reva();
        });
        Semaphore s = new Semaphore(1);
        s.release();

        //val.valueLabel.revalidate();

        log.info("Value of {} changed: [{}]", id, value);
    }
}
