package com.programm.plugz.api.lifecycle;

import java.lang.annotation.Annotation;

public enum LifecycleState {

    PRE_SETUP       ("pre-setup",       PreSetup.class),
    POST_SETUP      ("post-setup",      PostSetup.class),
    PRE_STARTUP     ("pre-startup",     PreStartup.class),
    POST_STARTUP    ("post-startup",    PostStartup.class),
    PRE_SHUTDOWN    ("pre-shutdown",    PreShutdown.class),
    POST_SHUTDOWN   ("post-shutdown",   PostShutdown.class)
    ;


    private final String name;
    public final Class<? extends Annotation> methodAnnotation;

    LifecycleState(String name, Class<? extends Annotation> methodAnnotation) {
        this.name = name;
        this.methodAnnotation = methodAnnotation;
    }

    @Override
    public String toString() {
        return name;
    }
}
