package com.programm.projects.plugz.simple.webserv;

import com.programm.projects.plugz.magic.api.web.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@ToString
public class RequestInfo {

    public final RequestType type;
    public final String fullQuery;
    public final String query;
    public final List<String> requestParamList;
    public final Map<String, String> requestParams;
    public final Map<String, String> headers;
}
