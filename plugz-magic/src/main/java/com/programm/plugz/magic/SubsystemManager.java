package com.programm.plugz.magic;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.instance.IAnnotatedClassSetup;
import com.programm.plugz.api.instance.IAnnotatedFieldSetup;
import com.programm.plugz.api.instance.IAnnotatedMethodSetup;
import com.programm.plugz.inject.PlugzUrlClassScanner;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Logger("Subsystems")
class SubsystemManager implements ISubsystemSetupHelper {

    private final ILogger log;
    private final PlugzUrlClassScanner scanner;
    private final AnnotationChecker annocheck;
    private final MagicInstanceManager instanceManager;
    private final List<ISubsystem> subsystems = new ArrayList<>();
    private final Map<Class<? extends Annotation>, IAnnotatedClassSetup<?>> classFoundHandlers = new HashMap<>();

    public void prepare() throws MagicInstanceException {
        List<Class<?>> subsystemImplementationClasses = scanner.getImplementing(ISubsystem.class);
        logSubsystemsFound(subsystemImplementationClasses);

        for(Class<?> subsystemCls : subsystemImplementationClasses) {
            log.debug("Instantiating subsystem class: [{}]...", subsystemCls.getName());

            try {
                instanceManager.instantiate(subsystemCls, subsystemInstance -> {
                    if(subsystemInstance instanceof ISubsystem subsystem) {
                        subsystems.add(subsystem);
                        try {
                            subsystem.registerSetup(this, annocheck);
                        }
                        catch (MagicException e){
                            throw new MagicInstanceException(e);
                        }
                    }
                    else {
                        throw new IllegalStateException("INVALID STATE: Instantiated object is does not implement the ISubsystem interface!");
                    }
                });
            }
            catch (MagicInstanceException e){
                throw new MagicInstanceException("Failed to instantiate subsystem class: [" + subsystemCls.getName() + "]!", e);
            }
        }
    }

    private void logSubsystemsFound(List<Class<?>> classes){
        log.info("Found [{}] subsystems:", classes.size());
        for(Class<?> cls : classes){
            log.info("-> {}", cls.getName());
        }
    }

    public void setupFoundClasses() throws MagicInstanceException {
        for(Map.Entry<Class<? extends Annotation>, IAnnotatedClassSetup<?>> entry : classFoundHandlers.entrySet()){
            Class<? extends Annotation> annotationCls = entry.getKey();
            IAnnotatedClassSetup<?> setupFunction = entry.getValue();

            List<Class<?>> annotatedClasses = scanner.getAnnotatedWith(annotationCls);
            for(Class<?> annotatedClass : annotatedClasses){
                Object annotationValue = annotatedClass.getAnnotation(annotationCls);
                setupFunction._setup(annotationValue, annotatedClass, instanceManager);
            }
        }
    }

    public void startup() throws MagicException {
        log.debug("Starting subsystems...");
        for(ISubsystem subsystem : subsystems){
            log.debug("-> {}", subsystem.getClass().getName());
            subsystem.startup();
        }
    }

    public void shutdown() throws MagicException {
        log.debug("Shutting down subsystems...");
        for(int i=subsystems.size()-1;i>=0;i--){
            ISubsystem subsystem = subsystems.get(i);
            log.debug("-> {}", subsystem.getClass().getName());
            subsystem.shutdown();
        }
    }

    @Override
    public <T extends Annotation> void registerClassAnnotation(Class<T> cls, IAnnotatedClassSetup<T> setup) {
        scanner.addSearchAnnotation(cls);
        classFoundHandlers.put(cls, setup);
    }

    @Override
    public <T extends Annotation> void registerFieldAnnotation(Class<T> cls, IAnnotatedFieldSetup<T> setup) {
        instanceManager.registerFieldSetup(cls, setup);
    }

    @Override
    public <T extends Annotation> void registerMethodAnnotation(Class<T> cls, IAnnotatedMethodSetup<T> setup) {
        instanceManager.registerMethodSetup(cls, setup);
    }

    @Override
    public void registerInstance(Class<?> cls, Object instance) throws MagicInstanceException{
        instanceManager.registerInstance(cls, instance);
    }
}
