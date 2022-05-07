package com.programm.plugz.webserv.api;

import com.programm.plugz.webserv.api.request.IRequest;

public interface IRequestValidator {

    boolean validate(IRequest request);

}
