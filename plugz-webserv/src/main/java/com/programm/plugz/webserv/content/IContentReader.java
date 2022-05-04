package com.programm.plugz.webserv.content;

import com.programm.plugz.object.mapper.ObjectMapException;

public interface IContentReader {

    <T> T read(String content, Class<T> cls) throws ObjectMapException;

}
