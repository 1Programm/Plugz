package com.programm.plugz.webserv;

import com.programm.plugz.api.instance.MagicMethod;
import com.programm.plugz.webserv.api.RequestParam;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class RequestMethodConfig {

    public final MagicMethod method;
//    public final RequestType type;
    public final String contentType;
    public final List<RequestParam> requestParamAnnotations;
    public final int requestBodyAnnotationPos;
    public final Class<?> requestBodyType;

}
