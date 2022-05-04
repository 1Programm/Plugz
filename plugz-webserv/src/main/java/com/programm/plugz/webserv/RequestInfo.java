package com.programm.plugz.webserv;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
class RequestInfo {

    public final RequestType type;
    public final String fullQuery;
    public final String query;
    public final List<String> requestParamList;
    public final Map<String, String> requestParams;
    public final Map<String, String> headers;

}
