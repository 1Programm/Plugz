package com.programm.plugz.webserv;

import com.programm.plugz.webserv.api.IRequestValidator;
import com.programm.plugz.webserv.api.config.*;
import com.programm.plugz.webserv.api.request.IExecutableRequest;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
class RestConfigImpl implements RestConfig {

    @RequiredArgsConstructor
    private static class RestPathConfigurableInterceptor implements IRequestInterceptor {
        private final List<IRequestValidator> validators;
        private final IRequestInterceptor onUnauthorizedAccess;
        private final IRequestInterceptor onSuccess;

        @Override
        public IInterceptedRequestAction onRequest(IRequestHandler requestHandler, IExecutableRequest request) throws InterceptPathException {
            for(IRequestValidator validator : validators){
                if(!validator.validate(request)){
                    return onUnauthorizedAccess.onRequest(requestHandler, request);
                }
            }

            return onSuccess.onRequest(requestHandler, request);
        }
    }

    private class RestPathConfigImpl implements IRestPathConfig {
        private final String[] paths;
        private final List<IRequestValidator> validators = new ArrayList<>();
        private IRequestInterceptor onUnauthorizedAccess;

        public RestPathConfigImpl(String... paths) {
            this.paths = paths;
        }

        @Override
        public IRestPathConfig with(IRequestValidator validator) {
            validators.add(validator);
            return this;
        }

        @Override
        public IRestPathConfig onUnauthorizedAccess(IRequestInterceptor onUnauthorizedAccess) {
            this.onUnauthorizedAccess = onUnauthorizedAccess;
            return this;
        }

        @Override
        public void onSuccess(IRequestInterceptor onSuccess) {
            if(onUnauthorizedAccess == null){
                onUnauthorizedAccess = (handler, req) -> req.doCancel();
            }

            IRequestInterceptor interceptor = new RestPathConfigurableInterceptor(validators, onUnauthorizedAccess, onSuccess);

            for(String path : paths){
                webserver.registerInterceptor(path, interceptor);
            }
        }
    }

    private final Webserver webserver;

    @Override
    public IRestPathConfig forPath(String path) {
        return new RestPathConfigImpl(path);
    }

    @Override
    public IRestPathConfig forPaths(String... paths) {
        return new RestPathConfigImpl(paths);
    }

    @Override
    public void intercept(String[] paths, IRequestInterceptor interceptor) {
        for(String path : paths) {
            webserver.registerInterceptor(path, interceptor);
        }
    }
}
