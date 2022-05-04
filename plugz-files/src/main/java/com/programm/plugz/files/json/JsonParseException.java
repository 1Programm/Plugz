package com.programm.plugz.files.json;

import com.programm.plugz.files.ResourceParseException;

public class JsonParseException extends ResourceParseException {

    public JsonParseException(String message) {
        super(message);
    }

    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonParseException(Throwable cause) {
        super(cause);
    }
}
