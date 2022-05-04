package com.programm.plugz.webserv.content;

import com.programm.plugz.object.mapper.ObjectMapException;

public interface IContentWriter {

    String write(Object object) throws ObjectMapException;

}
