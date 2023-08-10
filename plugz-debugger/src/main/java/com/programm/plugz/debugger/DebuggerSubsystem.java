package com.programm.plugz.debugger;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.api.utils.ValueUtils;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Logger("Debugger")
class DebuggerSubsystem implements ISubsystem {

    static {
        UIManager.put("TabbedPane.foreground", Color.BLACK);
    }

    private static final String CONF_FPS_SLEEP_NAME = "debugger.sleep.fps";
    private static final long CONF_FPS_SLEEP_DEFAULT = 100;

    private static final String CONF_LONG_SLEEP_NAME = "debugger.sleep.long";
    private static final long CONF_LONG_SLEEP_DEFAULT = 1000;

    private final ILogger log;
    private final IAsyncManager asyncManager;
    private final DebuggerWindow window;
    private final DebugUpdater updater;

    public DebuggerSubsystem(@Get ILogger log, @Get PlugzConfig config, @Get IAsyncManager asyncManager){
        this.log = log;
        this.asyncManager = asyncManager;
        this.window = new DebuggerWindow(asyncManager);

        long fpsSleepMillis = config.getLongOrRegisterDefault(CONF_FPS_SLEEP_NAME, CONF_FPS_SLEEP_DEFAULT);
        long longSleepMillis = config.getLongOrRegisterDefault(CONF_LONG_SLEEP_NAME, CONF_LONG_SLEEP_DEFAULT);
        this.updater = new DebugUpdater(window, fpsSleepMillis, longSleepMillis);
    }

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) {
        setupHelper.registerFieldAnnotation(DebugValue.class, this::setupDebugValueField);
    }

    @Override
    public void startup() {
        SwingUtilities.invokeLater(() -> {
            asyncManager.notifyCurrentNewThread();
            window.init();
            PrintStream err = System.err;
            System.setErr(new UIInitErrorPrintStreamFilter(err));
            window.setVisible(true);
            System.setErr(err);
        });
        asyncManager.runAsyncTask(updater::startup, null, 0, true, true);
    }

    @Override
    public void shutdown() {
        updater.shutdown();
        window.setVisible(false);
        window.dispose();
    }

    private void setupDebugValueField(DebugValue annotation, Object instance, Field field, IInstanceManager manager) {
        log.debug("DebugValue: {}", field);

        String debugName = annotation.value();
        if(debugName.isEmpty()){
            debugName = field.getDeclaringClass().getSimpleName() + "#" + field.getName();
        }

        MagicDebugValue magicDebugValue = getDebugValue(instance, field, debugName, manager);
        window.tabValues.addDebugValue(magicDebugValue);
    }

    private MagicDebugValue getDebugValue(Object instance, Field field, String debugName, IInstanceManager manager){
        Class<?> type = field.getType();
        boolean needsAccess = !Modifier.isPublic(field.getModifiers());
        DValue<?> debugValueInstance = null;

        List<MagicDebugValue> childrenList = new ArrayList<>();
        if(instance != null) {
            Object theInstance;

            if(Modifier.isStatic(field.getModifiers())){
                theInstance = manager.getField(field, null);
            }
            else {
                theInstance = manager.getField(field, instance);
            }

            if(DValue.class.isAssignableFrom(type)){
                debugValueInstance = (DValue<?>) theInstance;
                type = debugValueInstance.type;
                theInstance = debugValueInstance.value;
            }

            if(isValidField(type)) {
                Field[] fields = type.getDeclaredFields();
                for(Field childField : fields){
                    String childName = childField.getName();
                    if(childField.isAnnotationPresent(DebugValue.class)){
                        DebugValue debugValueAnnotation = childField.getAnnotation(DebugValue.class);
                        String _childName = debugValueAnnotation.value();
                        if(!_childName.isEmpty()){
                            childName = _childName;
                        }
                    }
                    MagicDebugValue childValue = getDebugValue(theInstance, childField, childName, manager);
                    childrenList.add(childValue);
                }
            }
        }

        MagicDebugValue[] children = childrenList.toArray(new MagicDebugValue[0]);
        return new MagicDebugValue(instance, field, needsAccess, debugName, debugValueInstance, children);
    }

    private boolean isValidField(Class<?> type){
        if(type == String.class) return false;
        type = ValueUtils.unwrapPrimitiveWrapper(type);
        return !type.isPrimitive();
    }
}
