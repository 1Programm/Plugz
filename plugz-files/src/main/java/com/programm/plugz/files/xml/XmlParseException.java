package com.programm.plugz.files.xml;

import com.programm.plugz.files.ResourceParseException;

public class XmlParseException extends ResourceParseException {

    public XmlParseException(String message) {
        super(message);
    }

    public XmlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public XmlParseException(Throwable cause) {
        super(cause);
    }
}
