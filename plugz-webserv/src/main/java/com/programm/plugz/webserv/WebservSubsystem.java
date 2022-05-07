package com.programm.plugz.webserv;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.api.instance.MagicMethod;
import com.programm.plugz.object.mapper.IObjectMapper;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.webserv.api.*;
import com.programm.plugz.webserv.api.config.RestConfig;
import com.programm.plugz.webserv.content.ContentHandler;
import com.programm.plugz.webserv.ex.WebservSetupException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

@Logger("Webserv-sys")
public class WebservSubsystem implements ISubsystem {

    private static final String CONF_SERVER_PORT_NAME = "webserv.port";
    private static final int CONF_SERVER_PORT_DEFAULT = 8080;

    private static final String CONF_SERVER_CLIENT_TIMEOUT_NAME = "webserv.client.timeout";
    private static final int CONF_SERVER_CLIENT_TIMEOUT_DEFAULT = 1000;

    private static final String CONF_SERVER_LOG_REGISTER_MAPPING_NAME = "webserv.log.mappings.register";
    private static final boolean CONF_SERVER_LOG_REGISTER_MAPPING_DEFAULT = false;

    private static final String CONF_SERVER_LOG_REQUESTS_NAME = "webserv.log.requests";
    private static final boolean CONF_SERVER_LOG_REQUESTS_DEFAULT = false;

    private final ILogger log;
    private final PlugzConfig config;
    private final IAsyncManager asyncManager;
    private final ContentHandler contentHandler;
    private final Webserver webserver;
    private final RestConfigImpl restConfig;

    private boolean logRegisterMappings;

    public WebservSubsystem(@Get ILogger log, @Get PlugzConfig config, @Get IAsyncManager asyncManager){
        this.log = log;
        this.config = config;
        this.asyncManager = asyncManager;
        this.contentHandler = new ContentHandler();
        this.webserver = new Webserver(log, contentHandler);
        this.restConfig = new RestConfigImpl(webserver);
    }

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException {
        config.registerDefaultConfiguration(CONF_SERVER_PORT_NAME, CONF_SERVER_PORT_DEFAULT);
        config.registerDefaultConfiguration(CONF_SERVER_CLIENT_TIMEOUT_NAME, CONF_SERVER_CLIENT_TIMEOUT_DEFAULT);
        this.logRegisterMappings = config.getBoolOrRegisterDefault(CONF_SERVER_LOG_REGISTER_MAPPING_NAME, CONF_SERVER_LOG_REGISTER_MAPPING_DEFAULT);
        config.registerDefaultConfiguration(CONF_SERVER_LOG_REQUESTS_NAME, CONF_SERVER_LOG_REQUESTS_DEFAULT);

        setupHelper.registerInstance(RestConfig.class, restConfig);

        setupHelper.registerSearchClass(IObjectMapper.class, this::registerImplementingObjectReaderClass);
        setupHelper.registerClassAnnotation(RestController.class, this::registerRestControllerClass);
        setupHelper.registerMethodAnnotation(GetMapping.class, this::registerGetMappingMethod);
        setupHelper.registerMethodAnnotation(PutMapping.class, this::registerPutMappingMethod);
        setupHelper.registerMethodAnnotation(PostMapping.class, this::registerPostMappingMethod);
        setupHelper.registerMethodAnnotation(DeleteMapping.class, this::registerDeleteMappingMethod);

        annocheck.forClass(GetMapping.class).classAnnotations().whitelist().set(RestController.class).seal();
        annocheck.forClass(PutMapping.class).classAnnotations().whitelist().set(RestController.class).seal();
        annocheck.forClass(PostMapping.class).classAnnotations().whitelist().set(RestController.class).seal();
        annocheck.forClass(DeleteMapping.class).classAnnotations().whitelist().set(RestController.class).seal();

        annocheck.forClass(RequestParam.class).classAnnotations().whitelist().set(RestController.class).seal();
        annocheck.forClass(RequestParam.class).partnerAnnotations().whitelist().seal();

        annocheck.forClass(RequestBody.class).classAnnotations().whitelist().set(RestController.class).seal();
        annocheck.forClass(RequestBody.class).partnerAnnotations().whitelist().seal();
    }

    @Override
    public void startup() throws MagicException {
        int port = config.getIntOrError(CONF_SERVER_PORT_NAME, WebservSetupException::new);
        int clientTimeout = config.getIntOrError(CONF_SERVER_CLIENT_TIMEOUT_NAME, WebservSetupException::new);
        boolean logRequests = config.getBoolOrError(CONF_SERVER_LOG_REQUESTS_NAME, WebservSetupException::new);

        log.info("Starting Server on port [{}]...", port);
        webserver.init(port, clientTimeout, logRequests);
        asyncManager.runAsyncTask(webserver::start, null, 0, true, false);
    }

    @Override
    public void shutdown() throws MagicException {
        webserver.stop();
    }

    private void registerImplementingObjectReaderClass(Class<?> implementingClass, IInstanceManager manager) throws MagicInstanceException {
        if(implementingClass != com.programm.plugz.object.mapper.property.JsonNodePropertyObjectMapper.class
        && implementingClass != com.programm.plugz.object.mapper.property.PropertyObjectJsonNodeMapper.class) {
            Class<?> _dataCls = null;
            Class<?> _entityCls = null;

            Type[] types = implementingClass.getGenericInterfaces();
            outer:
            for(Type interfaceType : types){
                if(interfaceType instanceof ParameterizedType parameterizedType){
                    Type rawType = parameterizedType.getRawType();
                    if(rawType == IObjectMapper.class){
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        for(Type actualType : actualTypeArguments){
                            if(actualType instanceof ParameterizedType || actualType instanceof  TypeVariable) throw new MagicInstanceException("Type variables in object mapper not allowed! [" + implementingClass.getName() + "]");
                            Class<?> actualTypeCls = (Class<?>) actualType;

                            if(_dataCls == null) {
                                _dataCls = actualTypeCls;
                            }
                            else {
                                _entityCls = actualTypeCls;
                                break outer;
                            }
                        }
                    }
                }
            }

            if(_entityCls == null) throw new IllegalStateException("INVALID STATE");

            final Class<?> dataCls = _dataCls;
            final Class<?> entityCls = _entityCls;

            if(logRegisterMappings) log.info("[Object Mapper]: {} --- ({} -> {})", implementingClass.getName(), dataCls.getName(), entityCls.getName());

            manager.instantiate(implementingClass, instance -> {
                try {
                    contentHandler.registerSpecializedMapper(dataCls, entityCls, (IObjectMapper<?, ?>) instance);
                }
                catch (ObjectMapException e){
                    throw new MagicInstanceException("Could not register specialized mapper [" + implementingClass.getName() + "]!", e);
                }
            });
        }
    }

    private void registerRestControllerClass(RestController annotation, Class<?> cls, IInstanceManager manager) throws MagicInstanceException {
        manager.instantiate(cls);
    }

    private void registerGetMappingMethod(GetMapping annotation, Object instance, Method method, IInstanceManager manager) throws MagicInstanceException {
        registerMappingMethod(instance, method, manager, RequestType.GET, annotation.value(), annotation.contentType());
    }

    private void registerPutMappingMethod(PutMapping annotation, Object instance, Method method, IInstanceManager manager) throws MagicInstanceException {
        registerMappingMethod(instance, method, manager, RequestType.PUT, annotation.value(), annotation.contentType());
    }

    private void registerPostMappingMethod(PostMapping annotation, Object instance, Method method, IInstanceManager manager) throws MagicInstanceException {
        registerMappingMethod(instance, method, manager, RequestType.POST, annotation.value(), annotation.contentType());
    }

    private void registerDeleteMappingMethod(DeleteMapping annotation, Object instance, Method method, IInstanceManager manager) throws MagicInstanceException {
        registerMappingMethod(instance, method, manager, RequestType.DELETE, annotation.value(), annotation.contentType());
    }

    private void registerMappingMethod(Object instance, Method method, IInstanceManager manager, RequestType type, String value, String contentType) throws MagicInstanceException {
        RestController restControllerAnnotation = instance.getClass().getAnnotation(RestController.class);
        String path = WebUtils.concatPathMapping(restControllerAnnotation.value(), value);
        if(path.charAt(0) != '/') path = "/" + path;

        if(logRegisterMappings) log.info("Register mapping: %30<([{}]) for {}.", path, type);

        if(contentType.isEmpty()){
            contentType = restControllerAnnotation.defaultContentType();
        }

        if(!contentHandler.supportsMimeType(contentType)){
            throw new MagicInstanceException("MIME-Type [" + contentType + "] is not supported!");
        }

        MagicMethod mm = manager.buildMagicMethod(instance, method);

        List<RequestParam> requestParams = new ArrayList<>();
        int requestBodyPos = -1;
        Class<?> requestBodyTyp = null;

        Annotation[][] annotations = method.getParameterAnnotations();
        Class<?>[] paramTypes = method.getParameterTypes();
        for(int i=0;i<method.getParameterCount();i++){
            Annotation[] paramAnnotations = annotations[i];
            for(int o=0;o<paramAnnotations.length;o++){
                if(paramAnnotations[o].annotationType() == RequestParam.class){
                    if(paramTypes[i] != String.class) throw new MagicInstanceException("RequestParams can only be of type String!");
                    requestParams.add((RequestParam) paramAnnotations[o]);
                    break;
                }
                else if(paramAnnotations[o].annotationType() == RequestBody.class){
                    if(requestBodyPos != -1) throw new MagicInstanceException("There can not be multiple RequestBody parameters!");
                    requestBodyPos = i;
                    requestBodyTyp = paramTypes[i];
                    break;
                }
            }
        }

        webserver.registerMapping(type, path, new RequestMethodConfig(mm, contentType, requestParams, requestBodyPos, requestBodyTyp));
    }
}
