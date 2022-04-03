package com.programm.projects.plugz.simple.webserv;

public interface IContentReader {

    <T> T read(String content, Class<T> cls) throws ContentMapException;

}
