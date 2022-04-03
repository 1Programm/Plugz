package com.programm.projects.plugz.magic.api.web;

import com.programm.projects.plugz.magic.api.IMagicMethod;

import java.util.List;

public abstract class WebRequestMethodConfig implements IMagicMethod {

    public final String path;
    public final RequestType type;
    public final String contentType;
    public final List<RequestParam> requestParamAnnotations;
    public final int requestBodyAnnotationPos;
    public final Class<?> requestBodyType;

    public WebRequestMethodConfig(String path, RequestType type, String contentType, List<RequestParam> requestParamAnnotations, int requestBodyAnnotationPos, Class<?> requestBodyType) {
        this.path = path;
        this.type = type;
        this.contentType = contentType;
        this.requestParamAnnotations = requestParamAnnotations;
        this.requestBodyAnnotationPos = requestBodyAnnotationPos;
        this.requestBodyType = requestBodyType;
    }
}
