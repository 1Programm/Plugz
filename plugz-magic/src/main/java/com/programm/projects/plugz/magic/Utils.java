package com.programm.projects.plugz.magic;

import java.net.URL;

class Utils {

    public static URL getUrlFromClass(Class<?> cls){
        return cls.getProtectionDomain().getCodeSource().getLocation();
    }

}
