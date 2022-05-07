package com.programm.plugz.webserv;

import com.programm.plugz.webserv.api.request.IUnprocessedRequest;
import com.programm.plugz.webserv.api.config.InterceptObjectMapException;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
class BasicRequest implements IUnprocessedRequest {

    protected final String origin;
    protected final RequestType type;
    protected final String query;
    protected final Map<String, String> params = new HashMap<>();
    protected final Map<String, String> headers = new HashMap<>();

    protected Object requestBody;

    @Override
    public String origin() {
        return origin;
    }

    @Override
    public RequestType type() {
        return type;
    }

    @Override
    public String query() {
        return query;
    }

    @Override
    public Map<String, String> params() {
        return params;
    }

    @Override
    public BasicRequest param(String name, String value){
        params.put(name, value);
        return this;
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public BasicRequest header(String name, String value){
        headers.put(name, value);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRequestBody(Class<T> cls) throws InterceptObjectMapException {
        if(requestBody == null) return null;
        if(cls.isAssignableFrom(requestBody.getClass())){
            return (T) requestBody;
        }

        throw new InterceptObjectMapException("Could not map request body of class [" + requestBody.getClass().getName() + "] to [" + cls + "]");
    }

    @Override
    public BasicRequest setRequestBody(Object requestBody){
        this.requestBody = requestBody;
        return this;
    }

    @Override
    public String buildFullQuery() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry : params.entrySet()){
            if(sb.length() == 0) sb.append("?");
            else sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return query + sb;
    }
}
