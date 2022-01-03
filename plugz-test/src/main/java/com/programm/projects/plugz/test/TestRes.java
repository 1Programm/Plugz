package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.Resource;
import lombok.ToString;

@Resource("log.properties")
@ToString
public class TestRes {

    public String logLevel;

}
