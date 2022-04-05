package com.programm.plugz.files.yaml;

import com.programm.plugz.files.ResourceParseException;

public class YamlParseException extends ResourceParseException {

    public YamlParseException(String message) {
        super(message);
    }

    public YamlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlParseException(Throwable cause) {
        super(cause);
    }
}
