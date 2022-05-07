package com.programm.plugz.test;

import com.programm.plugz.webserv.api.IRequestValidator;
import com.programm.plugz.webserv.api.request.IRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TestRequestValidator implements IRequestValidator {

    private final boolean valid;

    @Override
    public boolean validate(IRequest request) {
        return valid;
    }
}
