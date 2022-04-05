package com.programm.plugz.files.props;

import com.programm.plugz.files.ResourceParseException;

public class PropsParseException extends ResourceParseException {

    public PropsParseException(String message) {
        super(message);
    }

    public PropsParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropsParseException(Throwable cause) {
        super(cause);
    }
}
