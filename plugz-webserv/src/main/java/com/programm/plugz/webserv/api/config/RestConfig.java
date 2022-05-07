package com.programm.plugz.webserv.api.config;

public interface RestConfig {

    IRestPathConfig forPath(String path);

    IRestPathConfig forPaths(String... paths);

    void intercept(String[] paths, IRequestInterceptor interceptor);

    void fallbackInterceptor(IRequestInterceptor interceptor);

}
