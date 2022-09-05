package com.programm.plugz.webserv;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.webserv.api.RequestParam;
import com.programm.plugz.webserv.api.config.IInterceptedRequestAction;
import com.programm.plugz.webserv.api.config.IRequestHandler;
import com.programm.plugz.webserv.api.config.IRequestInterceptor;
import com.programm.plugz.webserv.api.config.InterceptedRequest;
import com.programm.plugz.webserv.api.request.IExecutableRequest;
import com.programm.plugz.webserv.api.request.IRequest;
import com.programm.plugz.webserv.api.request.IUnprocessedRequest;
import com.programm.plugz.webserv.api.request.InvalidRequestException;
import com.programm.plugz.webserv.content.ContentHandler;
import com.programm.plugz.webserv.content.IContentReader;
import com.programm.plugz.webserv.content.IContentWriter;
import com.programm.plugz.webserv.ex.WebservException;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        private final Map<String, List<String>> headers;
        private final Map<String, Cookie> cookies;
        private final Map<String, Cookie> newCookies = new HashMap<>();

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
        public Map<String, List<String>> headers() {
            return headers;
        }

        public String getFirstValueOfHeader(String name){
            List<String> values = headers.get(name);
            if(values == null) return null;
            return values.isEmpty() ? null : values.get(0);
        }

        @Override
        public Map<String, Cookie> cookies() {
            return cookies;
        }

        @Override
        public IExecutableRequest setCookie(Cookie cookie) {
            newCookies.put(cookie.name, cookie);
            return this;
        }

        @Override
        public <T> T getRequestBody(Class<T> cls) {
            return null; //TODO
        }

        @Override
        public String buildFullQuery() {
            return fullQuery;
        }

        @Override
        public IInterceptedRequestAction doContinue() {
            return new InterceptedRequestSupplierAction(null, null, newCookies, null, null, InterceptedRequest.Type.CONTINUE, null, null);
        }

        @Override
        public IInterceptedRequestAction doContinue(IUnprocessedRequest request) throws InvalidRequestException {
            String origin = request.origin();
            if(origin == null) throw new InvalidRequestException("Origin of request must be set for request-forwarding!");

            return new InterceptedRequestSupplierAction(origin, request, newCookies, null, null, InterceptedRequest.Type.FORWARD, null, null);
        }

        @Override
        public IInterceptedRequestAction doOk() {
            return new InterceptedRequestSupplierAction(null, null, newCookies, null, null, InterceptedRequest.Type.OK, null, null);
        }

        @Override
        public IInterceptedRequestAction doOk(String contentType, Object responseBody) {
            return new InterceptedRequestSupplierAction(null, null, newCookies, contentType, responseBody, InterceptedRequest.Type.OK, null, null);
        }

        @Override
        public IInterceptedRequestAction doOk(Object responseBody) {
            return new InterceptedRequestSupplierAction(null, null, newCookies, "application/json", responseBody, InterceptedRequest.Type.OK, null, null);
        }

        @Override
        public IInterceptedRequestAction doRedirect(IUnprocessedRequest request) throws InvalidRequestException {
            String origin = request.origin();
            return new InterceptedRequestSupplierAction(origin, request, newCookies, null, null, InterceptedRequest.Type.REDIRECT, null, null);
        }

        @Override
        public IInterceptedRequestAction doError(int status, String message) {
            return new InterceptedRequestSupplierAction(null, null, newCookies, null, null, InterceptedRequest.Type.ERROR, status, message);
        }
    }

    @RequiredArgsConstructor
    private static class InterceptedRequestSupplierAction implements IInterceptedRequestAction {
        private final String origin;
        private final IRequest request;
        private final Map<String, Cookie> newCookies;
        private final String contentType;
        private final Object responseBody;
        private final InterceptedRequest.Type type;
        private final Integer errStatus;
        private final String errMsg;

        @Override
        public InterceptedRequest run() {
            return new InterceptedRequest(origin, request, newCookies, contentType, responseBody, type, errStatus, errMsg);
        }
    }




    private final ILogger log;
    private final ContentHandler contentHandler;

    private final Map<RequestType, Map<String, List<RequestMethodConfig>>> mappings = new HashMap<>();
    private final Map<String, IRequestInterceptor> pathInterceptors = new HashMap<>();
    private IRequestInterceptor fallbackInterceptor;

    private int port;
    private int clientTimeout;
    private boolean logRequests;
    private boolean logFallback;

    private boolean running;

    public void init(int port, int clientTimeout, boolean logRequests, boolean logFallback){
        this.port = port;
        this.clientTimeout = clientTimeout;
        this.logRequests = logRequests;
        this.logFallback = logFallback;
    }

    public void start(){
        running = true;

        try(ServerSocket serverSocket = new ServerSocket(port)) {
            Thread extraThread = new Thread(() -> waitForClient(serverSocket), "Extra Thread");
            extraThread.start();
            waitForClient(serverSocket);
        }
        catch (IOException e){
            throw new MagicRuntimeException("Failed to start the server on port [" + port + "].", e);
        }
    }

    private void waitForClient(ServerSocket serverSocket){
        while(running) {
            try (Socket client = serverSocket.accept()) {
                client.setSoTimeout(clientTimeout);
                handleClient(client);
            } catch (IOException e) {
                log.logException("IOException when handling the client: " + e.getMessage(), e);
            }
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
        Map<String, List<String>> headers = new HashMap<>();
        Map<String, Cookie> cookies = new HashMap<>();

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
                in.readLine();

                init = false;
            }
            //Headers
            else {
                line = URLDecoder.decode(line, StandardCharsets.UTF_8);
                int firstColon = line.indexOf(':');

                String headerName = line.substring(0, firstColon);
                if(line.charAt(firstColon + 1) == ' ') firstColon++;
                String value = line.substring(firstColon + 1);
                headers.computeIfAbsent(headerName, h -> new ArrayList<>()).add(value);

                if(headerName.equals("Cookie")){
                    collectCookies(value, cookies);
                }
            }
        }

        if(requestParamList == null) requestParamList = new ArrayList<>();
        if(requestParameters == null) requestParameters = new HashMap<>();

        return new ExecutableRequestImpl(requestMethod, fullQuery, query, requestParameters, requestParamList, headers, cookies);
    }

    private void collectCookies(String value, Map<String, Cookie> cookieMap){
        String[] cookies = value.split("; ");

        for(String cookie : cookies){
            String[] cookieKeyValue = cookie.split("=");
            String cookieName = cookieKeyValue[0];
            String cookieValue = cookieKeyValue[1];
            cookieMap.put(cookieName, new Cookie(cookieName, cookieValue));
        }
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












    private void handleRequest(BufferedReader in, PrintWriter out, ExecutableRequestImpl request) {
        IRequestInterceptor interceptor = pathInterceptors.get(request.query);
        doOrDontInterceptRequest(in, out, request, interceptor, true);
    }

    private void doOrDontInterceptRequest(BufferedReader in, PrintWriter out, ExecutableRequestImpl request, IRequestInterceptor interceptor, boolean withFallback){
        if(interceptor == null) {
            continueHandleRequest(in, out, request, null, withFallback);
            return;
        }

        try {
            IInterceptedRequestAction action = interceptor.onRequest(this, request);
            InterceptedRequest interceptedRequest = action.run();

            String origin = interceptedRequest.getOrigin();
            InterceptedRequest.Type interceptType = interceptedRequest.getType();
            Map<String, Cookie> requestCookies = interceptedRequest.getNewCookies();

            switch (interceptType) {
                case OK ->  {
                    String contentType = interceptedRequest.getContentType();
                    Object responseBody = interceptedRequest.getResponseBody();

                    if(responseBody == null){
                        replyOk(out, requestCookies);
                    }
                    else {
                        IContentWriter writer = contentHandler.getWriter(contentType);
                        if (writer == null) throw new WebservException("No fitting writer for contentType [" + contentType + "] found!");

                        String _responseBody;
                        try {
                            _responseBody = writer.write(responseBody);
                        }
                        catch (ObjectMapException e) {
                            throw new WebservException("Failed to write content!", e);
                        }

                        replyOkData(out, contentType, requestCookies);
                        out.print(_responseBody);
                    }
                }
                case CONTINUE ->
                        continueHandleRequest(in, out, request, requestCookies, withFallback);
                case FORWARD ->
                        forwardRequest(out, origin, interceptedRequest.getRequest(), request.headers, requestCookies);
                case REDIRECT -> {
                    String redirectUrl = interceptedRequest.getRequest().buildFullQuery();
                    if(origin != null){
                        redirectUrl = WebUtils.concatPathMapping(origin, redirectUrl);
                    }

                    replyRedirect(out, redirectUrl, requestCookies);
                }
                case ERROR -> {
                    int errStatus = interceptedRequest.getErrStatus();
                    String errMsg = interceptedRequest.getErrMsg();
                    replyError(out, errStatus, errMsg, requestCookies);
                }
            }
        }
        catch (WebservException | IOException e){
            log.logException("Interceptor threw exception: " + e.getMessage(), e);
            replyError(out, 500, "Internal Server Error");
        }
    }

    private void continueHandleRequest(BufferedReader in, PrintWriter out, ExecutableRequestImpl request, Map<String, Cookie> newCookies, boolean withFallback){
        if(request.type == RequestType.OPTIONS){
            String requestingMethod = request.getFirstValueOfHeader("Access-Control-Request-Method");

            for(RequestType type : RequestType.values()){
                if(requestingMethod != null && requestingMethod.equals(type.name())){
                    if(invalidMapping(type, request.query)){
                        if(fallbackInterceptor != null && withFallback){
                            if(logFallback) log.info("[%7<({})]: {} -> Fallback interceptor.", request.type, request.fullQuery);
                            doOrDontInterceptRequest(in, out, request, fallbackInterceptor, false);
                            return;
                        }

                        if(logRequests) log.info("[%7<({})]: {} was rejected: No request mapper found!", request.type, request.query);

                        replyError(out, 501, "Not Implemented");
                        return;
                    }
                    break;
                }
            }

            replyOkOptions(out, newCookies);
        }
        else {
            try {
                handleRequest(out, in, request, newCookies, withFallback);
            }
            catch (WebservException | IOException e) {
                log.logException("Failed to do mapping [" + request.fullQuery + "]", e);
                replyError(out, 500, "Internal Server Error");
            }
        }
    }

    private void handleRequest(PrintWriter out, BufferedReader in, ExecutableRequestImpl request, Map<String, Cookie> newCookies, boolean withFallback) throws WebservException, IOException {
        List<RequestMethodConfig> configs = getConfigs(request.type, request.query);

        if(configs == null){
            if(fallbackInterceptor != null && withFallback){
                if(logFallback) log.info("[%7<({})]: {} -> Fallback interceptor.", request.type, request.fullQuery);
                doOrDontInterceptRequest(in, out, request, fallbackInterceptor, false);
                return;
            }

            if(logRequests) log.info("[%7<({})]: {} was rejected: No request mapper found!", request.type, request.fullQuery);
            replyError(out, 501, "Not Implemented");
            return;
        }

        for(RequestMethodConfig config : configs){
            handleRequestMappingForConfig(out, in, request, config, newCookies);
        }
    }

    private void handleRequestMappingForConfig(PrintWriter out, BufferedReader in, ExecutableRequestImpl request, RequestMethodConfig config, Map<String, Cookie> newCookies) throws WebservException, IOException {
        List<RequestParam> requestParams = config.requestParamAnnotations;
        Object[] params = new Object[requestParams.size() + (config.requestBodyAnnotationPos == -1 ? 0 : 1)];

        int bodyPos = config.requestBodyAnnotationPos;

        int countRequestParams = 0;
        for(int i=0;i<params.length;i++){
            if(i == bodyPos){
                String _cLength = request.getFirstValueOfHeader("Content-Length");
                if(_cLength == null) throw new WebservException("No Content-Length header defined but request body is requested!");
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

                String contentType = request.getFirstValueOfHeader("Content-Type");

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
            replyOk(out, newCookies);
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

        replyOkData(out, config.contentType, newCookies);
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

    public void registerFallbackInterceptor(IRequestInterceptor interceptor) {
        if(fallbackInterceptor != null) throw new IllegalStateException("Multiple fallback interceptors are not allowed!");
        this.fallbackInterceptor = interceptor;
    }



    private void forwardRequest(PrintWriter out, String origin, IRequest request, Map<String, List<String>> oldHeaders, Map<String, Cookie> newCookies) throws IOException, WebservException {
        URL url;
        try {
            url = new URL("http://localhost:8081/test");
        }
        catch (MalformedURLException e){
            throw new WebservException("Invalid url for forwarding: [" + origin + "]!", e);
        }




        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(request.type().name());
        //connection.setRequestProperty("Host", "localhost:4200");
        connection.setRequestProperty("Access-Control-Allow-Origin", "*");
        for(Map.Entry<String, List<String>> entry : oldHeaders.entrySet()){
            String key = entry.getKey();
            for(String val : entry.getValue()){
                connection.setRequestProperty(key, val);
            }
        }



        //connection.setRequestProperty("Accept-Charset", "UTF-8");

//        connection.setDoOutput(true);

        //String requestMethod = request.type().name();
        //connection.setRequestMethod(requestMethod);
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

//        long contentLength = connection.getContentLengthLong();
//
//        if(contentLength == 0){
//            replyOk(out, newCookies);
//            return;
//        }

//        OutputStream os = connection.getOutputStream();
//        PrintWriter writer = new PrintWriter(os);
//
//        writer.print("HTTP/1.0 " + request.type() + " " + request.buildFullQuery());
//        writer.print("Accept: application/json;charset=UTF-8\r\n");
//        writer.print("Accept-Charset: UTF-8\r\n");
//        writer.print("Access-Control-Allow-Origin: *\r\n");
//        writer.print("Access-Control-Allow-Headers: Content-Type\r\n");
//        writer.print("\r\n");


        //Get Response
        InputStream is;
        try {
            is = connection.getInputStream();
        }
        catch (IOException e){
            throw e;
        }
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
            replyOk(out, newCookies);
            return;
        }

        Map<String, List<String>> headers = connection.getHeaderFields();
        List<String> contentTypes = headers.get("Content-Type");
        String contentType = contentTypes == null ? null : (contentTypes.isEmpty() ? null : contentTypes.get(0));

        replyOkData(out, contentType, newCookies);
        out.print(_data);
    }














    private void printSetCookieHeaders(PrintWriter out, Map<String, Cookie> newCookies) {
        if(newCookies == null) return;
        for(Cookie cookie : newCookies.values()){
            out.print("Set-Cookie: " + cookie);
        }
    }

    private void replyOk(PrintWriter out, Map<String, Cookie> newCookies){
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        printSetCookieHeaders(out, newCookies);
        out.print("\r\n"); // End of headers
    }

    private void replyOkData(PrintWriter out, String dataType, Map<String, Cookie> newCookies){
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Content-Type: " + dataType + "\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        printSetCookieHeaders(out, newCookies);
        out.print("\r\n"); // End of headers
    }

    private void replyError(PrintWriter out, int status, String msg, Map<String, Cookie> newCookies){
        out.print("HTTP/1.0 " + status + " " + msg + "\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        printSetCookieHeaders(out, newCookies);
        out.print("\r\n");
    }

    private void replyError(PrintWriter out, int status, String msg){
        out.print("HTTP/1.0 " + status + " " + msg + "\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("\r\n");
    }

    private void replyOkOptions(PrintWriter out, Map<String, Cookie> newCookies){
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Accept: application/json;charset=UTF-8\r\n");
        out.print("Accept-Charset: UTF-8\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("Access-Control-Allow-Headers: Content-Type\r\n");
        printSetCookieHeaders(out, newCookies);
        out.print("\r\n"); // End of headers
    }

    private void replyRedirect(PrintWriter out, String url, Map<String, Cookie> newCookies){
        out.print("HTTP/1.0 303 See Other\r\n");
        out.print("Location: " + url + "\r\n");
        printSetCookieHeaders(out, newCookies);
        out.print("\r\n"); // End of headers
    }


    @Override
    public BasicRequest buildRequest(String origin, RequestType type, String path) {
        return new BasicRequest(origin, type, path);
    }

}
