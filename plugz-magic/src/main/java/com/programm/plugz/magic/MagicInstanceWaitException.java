package com.programm.plugz.magic;

import com.programm.plugz.api.MagicException;

public class MagicInstanceWaitException extends MagicException {

    public MagicInstanceWaitException(String message) {
        super(message);
    }

    public MagicInstanceWaitException(String message, Throwable cause) {
        super(message, cause);
    }

    public MagicInstanceWaitException(Throwable cause) {
        super(cause);
    }
}
