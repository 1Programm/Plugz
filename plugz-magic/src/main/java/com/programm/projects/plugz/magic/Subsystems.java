package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.inject.InjectManager;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.db.IDatabaseManager;
import com.programm.projects.plugz.magic.api.resources.IResourcesManager;
import com.programm.projects.plugz.magic.api.schedules.IScheduleManager;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Logger("Subsystems")
class Subsystems {

    private static final String[] NAMES = {
            "schedule-manager",
            "db-manager",
            "resource-manager"
    };

    private static final Map<String, Class<? extends ISubsystem>> subsystemClasses = new HashMap<>();

    static {
        int i = 0;
        subsystemClasses.put(NAMES[i++], IScheduleManager.class);
        subsystemClasses.put(NAMES[i++], IDatabaseManager.class);
        subsystemClasses.put(NAMES[i], IResourcesManager.class);
    }

    private final Map<String, ISubsystem> subsystemMap = new HashMap<>();
    private final ILogger log;

    public Subsystems(ILogger log) {
        this.log = log;
    }

    public <T extends ISubsystem> T getSubsystem(Class<T> cls){
        String name = null;

        for(String n : NAMES){
            Class<? extends ISubsystem> c = subsystemClasses.get(n);
            if(c == cls){
                name = n;
                break;
            }
        }

        if(name == null) throw new IllegalStateException("Invalid subsystem class: [" + cls + "]!");

        return cls.cast(subsystemMap.get(name));
    }

    @SuppressWarnings("unchecked")
    public void inject(IInstanceManager instanceManager, ThreadPoolManager threadPoolManager, InjectManager injectManager, boolean logSubsystemMissing){
        MagicInstanceManager subsystemInstanceManger = new MagicInstanceManager(threadPoolManager);

        //Needed instances for subsystems
        registerInstance(subsystemInstanceManger, ILogger.class, log);
        registerInstance(subsystemInstanceManger, IInstanceManager.class, instanceManager);
        registerInstance(subsystemInstanceManger, IAsyncManager.class, threadPoolManager);

        for(String name : NAMES){
            Class<? extends ISubsystem> subsystemClass = subsystemClasses.get(name);
            List<Class<? extends ISubsystem>> subsystemImpls = (List<Class<? extends ISubsystem>>)(List<?>)injectManager.findImplementations(subsystemClass);

            if(subsystemImpls.size() == 1){
                Class<? extends ISubsystem> subsystemImplCls = subsystemImpls.get(0);
                log.info("Found implementation for subsystem [{}]: {}.", name, subsystemImplCls);

                try {
                    ISubsystem subsystem = subsystemInstanceManger.instantiate(subsystemImplCls);
                    subsystemMap.put(name, subsystem);
                }
                catch (MagicInstanceException e){
                    throw new MagicRuntimeException("Failed to instantiate subsystem [" + name + "]:", e);
                }
            }
            else if(subsystemImpls.size() == 0){
                if(logSubsystemMissing){
                    log.warn("No implementation found for subsystem [{}]!", name);
                }
            }
            else {
                throw new MagicRuntimeException("Multiple possible implementations found for subsystem [" + name + "]:\n" + subsystemImpls);
            }
        }

        try {
            subsystemInstanceManger.checkWaitMap();
        } catch (MagicInstanceException e) {
            throw new MagicRuntimeException("Waiting dependencies could not be resolved. " + e.getMessage());
        }
    }

    private void registerInstance(MagicInstanceManager manager, Class<?> cls, Object instance){
        try {
            URL fromUrl = Utils.getUrlFromClass(instance.getClass());
            manager.registerInstance(fromUrl, cls, instance);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Could not register instance of class: [" + cls.getName() + "]!", e);
        }
    }

    public void startup() {
        for(String name : NAMES){
            try {
                ISubsystem subsystem = subsystemMap.get(name);
                if(subsystem == null) continue;
                log.debug("Starting subsystem [{}]...", name);
                subsystem.startup();
            }
            catch (MagicException e){
                throw new MagicRuntimeException("Failed to start subsystem [" + name + "].", e);
            }
        }
    }

    public void shutdown() {
        for(String name : NAMES){
            try {
                ISubsystem subsystem = subsystemMap.get(name);
                if(subsystem == null) continue;
                log.debug("Shutting down subsystem [{}]...", name);
                subsystem.shutdown();
            }
            catch (MagicException e){
                throw new MagicRuntimeException("Failed to shut down subsystem [" + name + "].", e);
            }
        }
    }

}
