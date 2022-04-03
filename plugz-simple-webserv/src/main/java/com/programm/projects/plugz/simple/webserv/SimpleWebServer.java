package com.programm.projects.plugz.simple.webserv;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.web.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Logger("Simple-Webserv")
public class SimpleWebServer implements IWebServerManager {

    @Get private ILogger log;
    @Get private IAsyncManager asyncManager;
    @Get private SysArgs sysArgs;

    private int port;
    private boolean logRequests;
    private boolean logRegisterMethods;
    private boolean running;

    private final Map<String, IContentWriter> contentWriterMap = new HashMap<>();
    private final Map<String, IContentReader> contentReaderMap = new HashMap<>();
    {
        contentWriterMap.put("application/json", new JsonContentWriter());
        contentWriterMap.put("application/text", Object::toString);

        contentReaderMap.put("application/json", new JsonContentReader());
        contentReaderMap.put("application/x-www-form-urlencoded", new PrimitiveContentReader());
    }

    private final Map<String, WebRequestMethodConfig> getMappings = new HashMap<>();
    private final Map<String, WebRequestMethodConfig> postMappings = new HashMap<>();

    @Override
    public void startup() throws MagicException {
        this.port = sysArgs.getDefault("-webserv.port", 8080);
        this.logRequests = sysArgs.getDefault("-webserv.log.requests", false);
        this.logRegisterMethods = sysArgs.getDefault("-webserv.log.register-methods", true);

        log.info("Starting server on port [{}]...", port);
        asyncManager.runAsyncVipTask(this::runServer, 0);
    }

    @Override
    public void shutdown() throws MagicException {
        log.info("Stopping server...");
        running = false;
    }

    @Override
    public void registerRequestMethod(WebRequestMethodConfig methodConfig) throws MagicWebException {
        if(logRegisterMethods) log.info("Registering-Method: [%7<({})]: {}", methodConfig.type, methodConfig.path);

        switch (methodConfig.type){
            case GET:
                getMappings.put(methodConfig.path, methodConfig);
                break;
            case POST:
                postMappings.put(methodConfig.path, methodConfig);
                break;
        }
    }

    private void runServer(){
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                try (Socket client = serverSocket.accept()) {
                    //FIX SO SPECIAL CHARACTERS WHICH ARE (IDKWHY) INTERPRETED INCORRECTLY WILL NOT RESULT IN INFINITE WAIT ON br.read()!!!
                    client.setSoTimeout(1000);
                    handleClient(client);
                }
            }
        }
        catch (IOException e){
            throw new MagicRuntimeException("Failed to start server socket on port: [" + port + "].", e);
        }
    }

    private void handleClient(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(socket.getOutputStream());

        RequestInfo request = parseRequest(in);

        if(logRequests) log.info("[%7<({})]: {}", request.type, request.fullQuery);

        handleRequest(in, out, request);
    }

    private RequestInfo parseRequest(BufferedReader in) throws IOException{
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

        return new RequestInfo(requestMethod, fullQuery, query, requestParamList, requestParameters, headers);
    }

    private void handleRequest(BufferedReader in, PrintWriter out, RequestInfo info) throws IOException {
        if(info.type == RequestType.OPTIONS){
            String requestingMethod = info.headers.get("Access-Control-Request-Method");

            if(requestingMethod.equals("POST")){
                if(!postMappings.containsKey(info.query)){
                    replyError(out, "501 Not Implemented");
                    out.close();
                    return;
                }
            }
            else if(requestingMethod.equals("GET")){
                if(!getMappings.containsKey(info.query)){
                    replyError(out, "501 Not Implemented");
                    out.close();
                    return;
                }
            }
            else {
                throw new IllegalStateException("NOT IMPLEMENTED!");
            }

            replyOkOptions(out);
            out.close();
        }
        else {
            try {
                doMapping(out, in, info);
            } catch (MagicWebException e) {
                log.error(e.getMessage());
                e.printStackTrace();
                replyError(out, "500 Internal Server Error");
            }

            out.close();
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

    private void replyError(PrintWriter out, String status){
        out.print("HTTP/1.0 " + status + "\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("\r\n");
    }

    private void replyOkOptions(PrintWriter out){
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Server: BackendServer/1.0\r\n");
        //out.print("Content-Type: application/json\r\n");
        out.print("Accept: application/json;charset=UTF-8\r\n");
        out.print("Accept-Charset: UTF-8\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        //out.print("Access-Control-Allow-Headers: Access-Control-Allow-Headers, Access-Control-Allow-Origin, Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers\r\n");
        out.print("Access-Control-Allow-Headers: Content-Type\r\n");
        out.print("\r\n"); // End of headers
    }

    private void doMapping(PrintWriter out, BufferedReader in, RequestInfo info) throws MagicWebException, IOException {
        String path = info.query;
        WebRequestMethodConfig config = null;

        if(info.type == RequestType.GET) {
            config = getMappings.get(path);
        }
        else if(info.type == RequestType.POST){
            config = postMappings.get(path);
        }

        if(config == null){
            replyError(out, "501 Not Implemented");
            return;
        }

        List<RequestParam> requestParams = config.requestParamAnnotations;
        Object[] params = new Object[requestParams.size() + (config.requestBodyAnnotationPos == -1 ? 0 : 1)];

        int bodyPos = config.requestBodyAnnotationPos;

        int countRequestParams = 0;
        for(int i=0;i<params.length;i++){
            if(i == bodyPos){
                String _cLength = info.headers.get("Content-Length");
                int cLength = Integer.parseInt(_cLength);

                StringBuilder contentBuilder = new StringBuilder();

                try {
                    for (int o = 0; o < cLength; o++) {
                        char c = (char) in.read();
                        contentBuilder.append(c);
                    }
                }
                catch (SocketTimeoutException e){
                    log.error("Could not read full content length. Probably 'Umlaute' are the problem! [{}] characters less.", (cLength - contentBuilder.length()));
                }

                String contentType = info.headers.get("Content-Type");

                IContentReader reader = contentReaderMap.get(contentType);

                if(reader == null) throw new MagicWebException("No fitting reader for contentType [" + config.contentType + "] found!");

                Object requestBody;
                try {
                    requestBody = reader.read(contentBuilder.toString(), config.requestBodyType);
                }
                catch (ContentMapException e){
                    throw new MagicWebException("Failed to read request body!", e);
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

                if(info.requestParams != null){
                    if (paramName.isEmpty()) {
                        if(countRequestParams < info.requestParamList.size()) {
                            params[i] = info.requestParamList.get(countRequestParams);
                        }
                    } else {
                        params[i] = info.requestParams.get(paramName);
                    }
                }

                countRequestParams++;
            }
        }

        Object data;
        try {
            data = config.call(params);
        }
        catch (MagicInstanceException e){
            throw new MagicWebException("Exception calling request-map method!", e);
        }

        if(data == null){
            replyOk(out);
            return;
        }

        IContentWriter writer = contentWriterMap.get(config.contentType);
        if (writer == null) throw new MagicWebException("No fitting writer for contentType [" + config.contentType + "] found!");

        String _data;
        try {
            _data = writer.write(data);
        } catch (ContentMapException e) {
            throw new MagicWebException("Failed to write content!", e);
        }

        if(info.type == RequestType.GET){
            replyOkData(out, config.contentType);
            out.print(_data);
        }
        else if(info.type == RequestType.POST){
            replyOkData(out, config.contentType);
            out.print(_data);
        }
    }

}
