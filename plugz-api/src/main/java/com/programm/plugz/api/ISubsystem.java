package com.programm.plugz.api;

import com.programm.plugz.annocheck.AnnotationChecker;

/**
 * A subsystem is a core part of plugz.
 * Each subsystem is separated into its own module.
 */
public interface ISubsystem {

    void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException;

    /**
     * A subsystem should use this method instead of the Magic-Lifecycle-Methods as it would mess up the order.
     * @throws MagicException if some exception happened.
     */
    void startup() throws MagicException;

    /**
     * A subsystem should use this method instead of the Magic-Lifecycle-Methods as it would mess up the order.
     * @throws MagicException if some exception happened.
     */
    void shutdown() throws MagicException;

}
