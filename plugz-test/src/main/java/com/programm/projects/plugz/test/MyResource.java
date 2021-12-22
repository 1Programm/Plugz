package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.Resource;
import lombok.ToString;

@Resource("log-default.properties")
@Resource(value = "log.properties", onexit = Resource.ONEXIT_SAVE)
@ToString
public class MyResource {
    public String logLevel;
    public String logFormat;
}
