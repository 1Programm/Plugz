package com.programm.plugz.events;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.api.instance.MagicMethod;

import java.lang.reflect.Method;

class EventSubsystem implements ISubsystem {

    private static final String CONF_EVENT_HANDLER_ENABLED_NAME = "events.handler.enabled";
    private static final boolean CONF_EVENT_HANDLER_ENABLED_DEFAULT = false;

    @Get private ILogger log;
    @Get private IAsyncManager asyncManager;
    private final EventHandlerImpl eventHandler;
    private final boolean eventHandlerEnabled;

    public EventSubsystem(@Get PlugzConfig config) {
        eventHandlerEnabled = config.getBoolOrRegisterDefault(CONF_EVENT_HANDLER_ENABLED_NAME, CONF_EVENT_HANDLER_ENABLED_DEFAULT);
        this.eventHandler = new EventHandlerImpl(eventHandlerEnabled);
    }

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException {
        setupHelper.registerInstance(IEventHandler.class, eventHandler);
        setupHelper.registerMethodAnnotation(Event.class, this::registerEventMethod);
    }

    @Override
    public void startup() {
        if(eventHandlerEnabled){
            asyncManager.runAsyncTask(eventHandler::handleEvents, null, 0, true, true);
        }
    }

    @Override
    public void shutdown() {
        eventHandler.eventHandlerRunning = false;
    }

    private void registerEventMethod(Event annotation, Object instance, Method method, IInstanceManager manager) {
        String eventName = annotation.value();
        MagicMethod mm = manager.buildMagicMethod(instance, method);
        EventListenerMethodImpl eventListenerMethodWrapper = new EventListenerMethodImpl(mm, method);

        log.debug("Register event - listener - method [{}]", eventName);

        eventHandler.subscribe(eventName, eventListenerMethodWrapper);
    }
}
