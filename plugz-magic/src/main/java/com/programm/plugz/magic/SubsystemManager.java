package com.programm.plugz.magic;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.condition.IConditionTester;
import com.programm.plugz.api.instance.IAnnotatedClassSetup;
import com.programm.plugz.api.instance.IAnnotatedFieldSetup;
import com.programm.plugz.api.instance.IAnnotatedMethodSetup;
import com.programm.plugz.api.instance.ISearchClassSetup;
import com.programm.plugz.inject.ScanCriteria;
import com.programm.plugz.inject.UrlClassScanner;
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
    private final UrlClassScanner scanner;
    private final ConditionTesterProxy conditionTesterProxy;
    private final AnnotationChecker annocheck;
    private final MagicInstanceManager instanceManager;
    private final List<ISubsystem> subsystems = new ArrayList<>();
    private final Map<Class<? extends Annotation>, IAnnotatedClassSetup<?>> foundAnnotatedClassHandlers = new HashMap<>();
    private final Map<Class<? extends Annotation>, List<Class<?>>> foundAnnotatedClasses = new HashMap<>();
    private final Map<Class<?>, ISearchClassSetup> foundClassImplementationHandlers = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> foundClassImplementations = new HashMap<>();

    public void prepare(List<Class<?>> subsystemImplementationClasses) throws MagicInstanceException {
        logSubsystemsFound(subsystemImplementationClasses);

        for(Class<?> subsystemCls : subsystemImplementationClasses) {
            log.debug("Instantiating subsystem class: [{}]...", subsystemCls.getName());

            try {
                instanceManager.instantiate(subsystemCls, subsystemInstance -> {
                    if(subsystemInstance instanceof ISubsystem subsystem) {
                        if(subsystemInstance instanceof IConditionTester) conditionTesterProxy.tester = (IConditionTester) subsystemInstance;

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
        for(Map.Entry<Class<? extends Annotation>, IAnnotatedClassSetup<?>> entry : foundAnnotatedClassHandlers.entrySet()){
            Class<? extends Annotation> annotationClass = entry.getKey();
            IAnnotatedClassSetup<?> setupFunction = entry.getValue();

            List<Class<?>> annotatedClasses = foundAnnotatedClasses.get(annotationClass);
            for(Class<?> annotatedClass : annotatedClasses){
                try {
                    Object annotationValue = annotatedClass.getAnnotation(annotationClass);
                    setupFunction._setup(annotationValue, annotatedClass, instanceManager);
                }
                catch (MagicInstanceException e){
                    throw new MagicInstanceException("Failed to set up discovered class: [" + annotatedClass.getName() + "]!", e);
                }
            }
        }

        for(Map.Entry<Class<?>, ISearchClassSetup> entry : foundClassImplementationHandlers.entrySet()){
            Class<?> searchClass = entry.getKey();
            ISearchClassSetup setupFunction = entry.getValue();

            List<Class<?>> implementingClasses = foundClassImplementations.get(searchClass);
            for(Class<?> implementingClass : implementingClasses){
                try {
                    setupFunction.setup(implementingClass, instanceManager);
                }
                catch (MagicInstanceException e){
                    throw new MagicInstanceException("Failed to set up discovered class: [" + implementingClass.getName() + "]!", e);
                }
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
    public void registerSearchClass(Class<?> cls, ISearchClassSetup setup) {
        List<Class<?>> foundClasses = new ArrayList<>();
        scanner.withCriteria(ScanCriteria.createOnSuccessCollect("Implementing " + cls.getName(), foundClasses).classImplements(cls));

        foundClassImplementationHandlers.put(cls, setup);
        foundClassImplementations.compute(cls, (c, l) -> {
            if(l == null) {
                return foundClasses;
            }
            else {
                l.addAll(foundClasses);
                return l;
            }
        });
    }

    @Override
    public <T extends Annotation> void registerClassAnnotation(Class<T> cls, IAnnotatedClassSetup<T> setup) {
        List<Class<?>> foundClasses = new ArrayList<>();
        scanner.withCriteria(ScanCriteria.createOnSuccessCollect("Annotated with " + cls.getName(), foundClasses).classAnnotatedWith(cls));

        foundAnnotatedClassHandlers.put(cls, setup);
        foundAnnotatedClasses.compute(cls, (c, l) -> {
            if(l == null) {
                return foundClasses;
            }
            else {
                l.addAll(foundClasses);
                return l;
            }
        });
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
