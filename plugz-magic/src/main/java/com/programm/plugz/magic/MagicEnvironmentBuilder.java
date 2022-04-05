package com.programm.plugz.magic;

import com.programm.plugz.inject.PlugzUrlClassScanner;
import com.programm.projects.ioutils.log.api.out.ILogger;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

class MagicEnvironmentBuilder {

    static MagicEnvironment create(String... args){
        MagicEnvironment magicEnvironment = new MagicEnvironment(args);

        ILogger logger = getLoggerImplementation();
        if(logger != null) magicEnvironment.setLogger(logger);

        return magicEnvironment;
    }

    private static ILogger getLoggerImplementation(){
        try {
            Class<?> cls = Class.forName("com.programm.projects.ioutils.log.logger.ConfLogger");
            return (ILogger) cls.getMethod("get").invoke(null);
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore){}

        PlugzUrlClassScanner logScanner = new PlugzUrlClassScanner();
        logScanner.addSearchClass(ILogger.class);
        List<Class<?>> loggerImplementations = logScanner.getImplementing(ILogger.class);

        if(loggerImplementations == null || loggerImplementations.isEmpty()) return null;

        return ILogger.class.cast(loggerImplementations.get(0));
    }

}
