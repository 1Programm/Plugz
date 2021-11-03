package com.programm.projects.plugz;

import java.io.*;
import java.util.Properties;

public class PlugzBuilder {

    public static final String PROP_ALLOWED_WRAPPERS = "plugins.wrappers.allowed";

    static Plugz fromConfigFile(String path){
        return fromConfigFile(new File(path));
    }

    static Plugz fromConfigFile(File file){
        if(!file.exists()) throw new IllegalArgumentException("File [" + file.getAbsolutePath() + "] does not exist!");

        Properties properties = new Properties();
        try (FileReader fr = new FileReader(file))
        {
            properties.load(fr);
        }
        catch (FileNotFoundException e){
            throw new IllegalArgumentException("File [" + file.getAbsolutePath() + "] does not exist!", e);
        }
        catch (IOException e){
            throw new IllegalArgumentException("Exception while reading file [" + file.getAbsolutePath() + "] as properties!", e);
        }

        return fromProperties(properties);
    }

    static Plugz fromInputStream(InputStream is){
        Properties properties = new Properties();
        try {
            properties.load(is);
        } catch (IOException e){
            throw new IllegalArgumentException("Exception while reading InputStream as properties!", e);
        }

        return fromProperties(properties);
    }

    static Plugz fromProperties(Properties properties){
        String _allowedWrapperTypes = properties.getProperty(PROP_ALLOWED_WRAPPERS);
        int allowedWrapperTypes = 0;
        if(_allowedWrapperTypes != null){
            allowedWrapperTypes = Integer.parseInt(_allowedWrapperTypes);
        }

        return build(allowedWrapperTypes);
    }

    private static Plugz build(int allowedWrapperTypes){
        if(allowedWrapperTypes == 0){
            Plugz.STATIC_LOG.println("[WARN]: No allowed wrapper types were specified. Could result in problems!");
        }

        return new Plugz(allowedWrapperTypes);
    }




    private int allowedWrapperTypes;

    PlugzBuilder(){}

    public PlugzBuilder allowWrapperType(PlugWrapperType type){
        this.allowedWrapperTypes |= type.val;
        return this;
    }

    public Plugz build(){
        return build(allowedWrapperTypes);
    }

}
