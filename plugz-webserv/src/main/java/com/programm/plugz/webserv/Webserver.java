package com.programm.plugz.webserv;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.webserv.api.RequestParam;
import com.programm.plugz.webserv.api.config.*;
import com.programm.plugz.webserv.api.request.IExecutableRequest;
import com.programm.plugz.webserv.api.request.IRequest;
import com.programm.plugz.webserv.api.request.IUnprocessedRequest;
import com.programm.plugz.webserv.content.ContentHandler;
import com.programm.plugz.webserv.content.IContentReader;
import com.programm.plugz.webserv.content.IContentWriter;
import com.programm.plugz.webserv.ex.WebservException;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RequiredArgsConstructor
@Logger("Webserv")
class Webserver implements IRequestHandler {

    @RequiredArgsConstructor
    private static class ExecutableRequestImpl implements IExecutableRequest {
        private final RequestType type;
        private final String fullQuery;
        private final String query;
        private final Map<String, String> params;
        private final List<String> paramsList;
        private final Map<String, String> headers;

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
        public Map<String, String> headers() {
            return headers;
        }

        @Override
        public <T> T getRequestBody(Class<T> cls) throws InterceptObjectMapException {
            return null;
        }

        @Override
        public String buildFullQuery() {
            return fullQuery;
        }

        @Override
        public IInterceptedRequestAction doContinue() {
            return new InterceptedDoContinueAction();
        }

        @Override
        public IInterceptedRequestAction doContinue(IUnprocessedRequest request) {
            return new InterceptedDoForwardAction(request);
        }

        @Override
        public IInterceptedRequestAction doCancel() {
            return new InterceptedDoCancelAction();
        }

        @Override
        public IInterceptedRequestAction doRedirect(IUnprocessedRequest newRequest) {
            return new InterceptedDoRedirectAction(newRequest);
        }

        @Override
        public IInterceptedRequestAction doError(int status, String message) {
            return new InterceptedDoErrorAction(status, message);
        }
    }

    @RequiredArgsConstructor
    private static class InterceptedDoContinueAction implements IInterceptedRequestAction {
        @Override
        public InterceptedRequest run() {
            return new InterceptedRequest(null, null, InterceptedRequest.Type.CONTINUE);
        }
    }

    @RequiredArgsConstructor
    private static class InterceptedDoForwardAction implements IInterceptedRequestAction {
        private final IUnprocessedRequest request;

        @Override
        public InterceptedRequest run() {
            return new InterceptedRequest(request.origin(), request, InterceptedRequest.Type.FORWARD);
        }
    }

    private static class InterceptedDoCancelAction implements IInterceptedRequestAction {
        @Override
        public InterceptedRequest run() {
            return null;
        }
    }

    @RequiredArgsConstructor
    private static class InterceptedDoRedirectAction implements IInterceptedRequestAction {
        private final IUnprocessedRequest request;

        @Override
        public InterceptedRequest run() {
            return new InterceptedRequest(request.origin(), request, InterceptedRequest.Type.REDIRECT);
        }
    }

    @RequiredArgsConstructor
    private static class InterceptedDoErrorAction implements IInterceptedRequestAction {
        private final int status;
        private final String msg;

        @Override
        public InterceptedRequest run() {
            return new InterceptedRequest(null, null, InterceptedRequest.Type.ERROR, status, msg);
        }
    }





    private final ILogger log;
    private final ContentHandler contentHandler;

    private final Map<RequestType, Map<String, List<RequestMethodConfig>>> mappings = new HashMap<>();
    private final Map<String, IRequestInterceptor> pathInterceptors = new HashMap<>();

    private int port;
    private int clientTimeout;
    private boolean logRequests;
    private boolean running;

    public void init(int port, int clientTimeout, boolean logRequests){
        this.port = port;
        this.clientTimeout = clientTimeout;
        this.logRequests = logRequests;
    }

    public void start(){
        running = true;

        try(ServerSocket serverSocket = new ServerSocket(port)) {
            while(running) {
                try(Socket client = serverSocket.accept()){
                    client.setSoTimeout(clientTimeout);
                    handleClient(client);
                }
                catch (IOException e){
                    log.logException("IOException when handling the client: " + e.getMessage(), e);
                }
            }
        }
        catch (IOException e){
            throw new MagicRuntimeException("Failed to start the server on port [" + port + "].", e);
        }
    }

    public void stop(){
        running = false;
    }

    private void handleClient(Socket client) throws IOException {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(client.getOutputStream()))
        {
            ExecutableRequestImpl request = parseRequest(in);
            if(logRequests) log.info("[%7<({})]: {}", request.type, request.fullQuery);
            handleRequest(in, out, request);
        }
    }

    private ExecutableRequestImpl parseRequest(BufferedReader in) throws IOException {
        boolean init = true;
        RequestType requestMethod = null;
        String fullQuery = null;
        String query = null;
        List<String> requestParamList = null;
        Map<String, String> requestParameters = null;
        Map<String, String> headers = new HashMap<>();

        String line;
        while (!(line = in.readLine()).isBlank()) {
            if(init){
                String[] split = line.split(" ");

                requestMethod = RequestType.valueOf(split[0]);
                fullQuery = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                String[] querySplit = fullQuery.split("\\?");
                query = querySplit[0];

                if(querySplit.length > 1){
                    requestParamList = new ArrayList<>();
                    requestParameters = getRequestParameters(querySplit[1], requestParamList);
                }

                //host line - don't need it yet
                line = in.readLine();

                init = false;
            }
            //Headers
            else {
                line = URLDecoder.decode(line, StandardCharsets.UTF_8);
                int firstColon = line.indexOf(':');

                String headerName = line.substring(0, firstColon);
                if(line.charAt(firstColon + 1) == ' ') firstColon++;
                String value = line.substring(firstColon + 1);
                headers.put(headerName, value);
            }
        }

        if(requestParamList == null) requestParamList = new ArrayList<>();
        if(requestParameters == null) requestParameters = new HashMap<>();

        return new ExecutableRequestImpl(requestMethod, fullQuery, query, requestParameters, requestParamList, headers);
    }

    private Map<String, String> getRequestParameters(String s, List<String> asList){
        Map<String, String> map = new HashMap<>();

        String[] split = s.split("&");
        for(String param : split){
            String[] keyVal = param.split("=");
            String key = keyVal[0];
            String val = keyVal.length > 1 ? keyVal[1] : "";

            asList.add(val);
            map.put(key, val);
        }

        return map;
    }

    private void handleRequest(BufferedReader in, PrintWriter out, ExecutableRequestImpl request) throws IOException {
        IRequestInterceptor pathInterceptor = pathInterceptors.get(request.query);
        if(pathInterceptor != null){
            try {
                IInterceptedRequestAction action = pathInterceptor.onRequest(this, request);

                InterceptedRequest interceptedRequest = action.run();

                if(interceptedRequest == null){
                    replyOk(out);
                    return;
                }

                String origin = interceptedRequest.getOrigin();
                InterceptedRequest.Type interceptType = interceptedRequest.getType();

                switch (interceptType) {
                    case CONTINUE -> {}
                    case FORWARD -> {
                        try {
                            forwardRequest(origin, interceptedRequest.getRequest(), out);
                        }
                        catch (MalformedURLException e){
                            log.logException(e);
                            replyError(out, 500, "Internal Server Error");
                        }
                        return;
                    }
                    case REDIRECT -> {
                        String redirectUrl = interceptedRequest.getRequest().buildFullQuery();
                        if(origin != null){
                            redirectUrl = WebUtils.concatPathMapping(origin, redirectUrl);
                        }

                        replyRedirect(out, redirectUrl);
                        return;
                    }
                    case ERROR -> {
                        int errStatus = interceptedRequest.getErrStatus();
                        String errMsg = interceptedRequest.getErrMsg();
                        replyError(out, errStatus, errMsg);
                        return;
                    }
                }
            }
            catch (InterceptPathException e){
                log.logException(e);
            }
        }

        if(request.type == RequestType.OPTIONS){
            String requestingMethod = request.headers.get("Access-Control-Request-Method");

            for(RequestType type : RequestType.values()){
                if(requestingMethod.equals(type.name())){
                    if(invalidMapping(type, request.query)){
                        if(logRequests) log.info("[%7<({})]: {} was rejected: No request mapper found!", request.type, request.query);
                        replyError(out, 501, "Not Implemented");
                        return;
                    }
                    break;
                }
            }

            replyOkOptions(out);
        }
        else {
            try {
                doMapping(out, in, request);
            } catch (WebservException e) {
                log.logException("Failed to do mapping [" + request.fullQuery + "]", e);
                replyError(out, 500, "Internal Server Error");
            }
        }
    }

    private void doMapping(PrintWriter out, BufferedReader in, ExecutableRequestImpl request) throws WebservException, IOException {
        List<RequestMethodConfig> configs = getConfigs(request.type, request.query);

        if(configs == null){
            if(logRequests) log.info("[%7<({})]: {} was rejected: No request mapper found!", request.type, request.fullQuery);
            replyError(out, 501, "Not Implemented");
            return;
        }

        for(RequestMethodConfig config : configs){
            doMethodMapping(out, in, request, config);
        }
    }

    private void doMethodMapping(PrintWriter out, BufferedReader in, ExecutableRequestImpl request, RequestMethodConfig config) throws WebservException, IOException {
        List<RequestParam> requestParams = config.requestParamAnnotations;
        Object[] params = new Object[requestParams.size() + (config.requestBodyAnnotationPos == -1 ? 0 : 1)];

        int bodyPos = config.requestBodyAnnotationPos;

        int countRequestParams = 0;
        for(int i=0;i<params.length;i++){
            if(i == bodyPos){
                String _cLength = request.headers.get("Content-Length");
                int cLength = Integer.parseInt(_cLength);

                StringBuilder contentBuilder = new StringBuilder();

                try {
                    for (int o=0;o<cLength;o++) {
                        char c = (char) in.read();
                        contentBuilder.append(c);
                    }
                }
                catch (SocketTimeoutException e){
                    log.error("Could not read full content length. Probably 'Umlaute' are the problem! [{}] characters less.", (cLength - contentBuilder.length()));
                }

                String contentType = request.headers.get("Content-Type");

                IContentReader reader = contentHandler.getReader(contentType);

                if(reader == null) throw new WebservException("No fitting reader for contentType [" + config.contentType + "] found!");

                Object requestBody;
                try {
                    requestBody = reader.read(contentBuilder.toString(), config.requestBodyType);
                }
                catch (ObjectMapException e){
                    throw new WebservException("Failed to read request body!", e);
                }

                params[i] = requestBody;
            }
            else if(requestParams.size() <= i){
                i--;
                bodyPos--;
            }
            else {
                RequestParam anno = requestParams.get(i);
                String paramName = anno.value();

                if (paramName.isEmpty()) {
                    if(countRequestParams < request.params.size()) {
                        params[i] = request.paramsList.get(countRequestParams);
                    }
                } else {
                    params[i] = request.params.get(paramName);
                }

                countRequestParams++;
            }
        }

        Object data;
        try {
            data = config.method.invoke(params);
        }
        catch (MagicInstanceException e){
            throw new WebservException("Exception calling request method for [" + request.fullQuery + "]!", e);
        }

        if(data == null){
            replyOk(out);
            return;
        }

        IContentWriter writer = contentHandler.getWriter(config.contentType);
        if (writer == null) throw new WebservException("No fitting writer for contentType [" + config.contentType + "] found!");

        String _data;
        try {
            _data = writer.write(data);
        } catch (ObjectMapException e) {
            throw new WebservException("Failed to write content!", e);
        }

        replyOkData(out, config.contentType);
        out.print(_data);
    }

    private boolean invalidMapping(RequestType type, String path){
        Map<String, List<RequestMethodConfig>> specificMappings = mappings.get(type);

        if(specificMappings == null) return true;
        return !specificMappings.containsKey(path);
    }

    private List<RequestMethodConfig> getConfigs(RequestType type, String path){
        Map<String, List<RequestMethodConfig>> specificMappings = mappings.get(type);

        if(specificMappings == null) return null;
        return specificMappings.get(path);
    }

    public void registerMapping(RequestType type, String path, RequestMethodConfig methodConfig){
        mappings.computeIfAbsent(type, t -> new HashMap<>())
                .computeIfAbsent(path, p -> new ArrayList<>())
                .add(methodConfig);
    }

    public void registerInterceptor(String path, IRequestInterceptor interceptor) {
        if(pathInterceptors.containsKey(path)) throw new IllegalStateException("Multiple interceptors registered for path [" + path + "]!");
        pathInterceptors.put(path, interceptor);
    }



    private void forwardRequest(String origin, IRequest request, PrintWriter out) throws IOException {
        String redirectUrl = request.buildFullQuery();
        if(origin != null){
            redirectUrl = WebUtils.concatPathMapping(origin, redirectUrl);
        }

        URL url = new URL(redirectUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String requestMethod = request.type().name();
        connection.setRequestMethod(requestMethod);
//        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//        connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
//        connection.setRequestProperty("Content-Language", "en-US");

        //connection.setUseCaches(false);
        //connection.setDoOutput(true);

        //Send request
//        DataOutputStream wr = new DataOutputStream (
//                connection.getOutputStream());
//        wr.writeBytes(urlParameters);
//        wr.close();

        long contentLength = connection.getContentLengthLong();

        if(contentLength == 0){
            replyOk(out);
            return;
        }

        //Get Response
        InputStream is = connection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        br.close();

        String _data = response.toString();

        if(_data.isEmpty()){
            replyOk(out);
            return;
        }

        Map<String, List<String>> headers = connection.getHeaderFields();
        List<String> contentTypes = headers.get("Content-Type");
        String contentType = contentTypes == null ? null : (contentTypes.isEmpty() ? null : contentTypes.get(0));

        replyOkData(out, contentType);
        out.print(_data);
    }
















    private void replyOk(PrintWriter out){
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("\r\n"); // End of headers
    }

    private void replyOkData(PrintWriter out, String dataType){
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Content-Type: " + dataType + "\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("\r\n"); // End of headers
    }

    private void replyError(PrintWriter out, int status, String msg){
        out.print("HTTP/1.0 " + status + " " + msg + "\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("\r\n");
    }

    private void replyOkOptions(PrintWriter out){
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Accept: application/json;charset=UTF-8\r\n");
        out.print("Accept-Charset: UTF-8\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("Access-Control-Allow-Headers: Content-Type\r\n");
        out.print("\r\n"); // End of headers
    }

    private void replyRedirect(PrintWriter out, String url){
        out.print("HTTP/1.0 303 See Other\r\n");
        out.print("Location: " + url + "\r\n");
        out.print("\r\n"); // End of headers
    }


    @Override
    public BasicRequest buildRequest(String origin, RequestType type, String path) {
        return new BasicRequest(origin, type, path);
    }
}
