package com.programm.plugz.debugger;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;

import javax.swing.*;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Logger("Debugger")
class DebuggerSubsystem implements ISubsystem {

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

        long fpsSleepMillis = config.getLongOrDefault(CONF_FPS_SLEEP_NAME, CONF_FPS_SLEEP_DEFAULT);
        long longSleepMillis = config.getLongOrDefault(CONF_LONG_SLEEP_NAME, CONF_LONG_SLEEP_DEFAULT);
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

    private void setupDebugValueField(DebugValue annotation, Object instance, Field field, IInstanceManager manager, PlugzConfig config) throws MagicInstanceException {
        log.debug("DebugValue: {}", field);

        String debugName = annotation.value();
        if(debugName.isEmpty()){
            debugName = field.getDeclaringClass().getSimpleName() + "#" + field.getName();
        }

        DValue<?> debugValueInstance = null;

        Class<?> type = field.getType();
        if(DValue.class.isAssignableFrom(type)){
            debugValueInstance = (DValue<?>) manager.getField(field, instance);
        }

        boolean needsAccess = !Modifier.isPublic(field.getModifiers());

        MagicDebugValue magicDebugValue = new MagicDebugValue(instance, field, needsAccess, debugName, debugValueInstance);
        window.addDebugValue(magicDebugValue);
    }
}
