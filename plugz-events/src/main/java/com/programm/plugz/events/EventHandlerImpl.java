package com.programm.plugz.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

class EventHandlerImpl implements IEventHandler {

    private class EventSubscriptionImpl implements IEventSubscription {
        private final String eventName;
        private final IEventListener listener;

        public EventSubscriptionImpl(String eventName, IEventListener listener) {
            this.eventName = eventName;
            this.listener = listener;
        }

        @Override
        public void unsubscribe() {
            EventHandlerImpl.this.unsubscribe(this);
        }
    }

    static class Event {
        private final String eventName;
        private final Object[] args;

        public Event(String eventName, Object[] args) {
            this.eventName = eventName;
            this.args = args;
        }
    }

    private final boolean eventHandlerEnabled;

    private final ReentrantLock listenersModifyLock = new ReentrantLock();
    private final Map<String, List<EventSubscriptionImpl>> eventListenersMap = new HashMap<>();
    final LinkedBlockingDeque<Event> eventQueue;

    boolean eventHandlerRunning;

    public EventHandlerImpl(boolean eventHandlerEnabled) {
        this.eventHandlerEnabled = eventHandlerEnabled;
        this.eventQueue = eventHandlerEnabled ? new LinkedBlockingDeque<>() : null;
    }

    @Override
    public void emit(String name, Object... args) {
        if(eventHandlerEnabled){
            Event event = new Event(name, args);
            eventQueue.add(event);
        }
        else {
            handleEvent(name, args);
        }
    }

    public void handleEvents(){
        eventHandlerRunning = true;

        try {
            while(eventHandlerRunning) {
                Event event = eventQueue.take();
                handleEvent(event.eventName, event.args);
            }
        }
        catch (InterruptedException e){
            eventHandlerRunning = false;
        }
    }

    private void handleEvent(String name, Object[] args){
        List<EventSubscriptionImpl> eventListeners = eventListenersMap.get(name);
        if(eventListeners != null){
            for(int i=0;i<eventListeners.size();i++){
                IEventListener listener = eventListeners.get(i).listener;
                listener.onEvent(args);
            }
        }
    }

    @Override
    public synchronized IEventSubscription subscribe(String eventName, IEventListener listener) {
        EventSubscriptionImpl subscription;

        listenersModifyLock.lock();
        try {
            List<EventSubscriptionImpl> subscriptions = eventListenersMap.computeIfAbsent(eventName, n -> new ArrayList<>());
            subscription = new EventSubscriptionImpl(eventName, listener);
            subscriptions.add(subscription);
        }
        finally {
            listenersModifyLock.unlock();
        }

        return subscription;
    }

    private synchronized void unsubscribe(EventSubscriptionImpl subscription){
        try {
            String eventName = subscription.eventName;
            List<EventSubscriptionImpl> subscriptions = eventListenersMap.computeIfAbsent(eventName, n -> new ArrayList<>());
            subscriptions.remove(subscription);
        }
        finally {
            listenersModifyLock.unlock();
        }
    }
}
