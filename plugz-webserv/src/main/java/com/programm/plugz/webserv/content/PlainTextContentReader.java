package com.programm.plugz.webserv.content;

import com.programm.plugz.api.utils.ValueUtils;

public class PlainTextContentReader implements IContentReader {

    @Override
    public <T> T read(String content, Class<T> cls) {
        return ValueUtils.parsePrimitive(content, cls);
    }
}
