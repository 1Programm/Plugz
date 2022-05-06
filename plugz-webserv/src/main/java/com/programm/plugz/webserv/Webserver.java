package com.programm.plugz.webserv;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.webserv.api.RequestParam;
import com.programm.plugz.webserv.content.ContentHandler;
import com.programm.plugz.webserv.content.IContentReader;
import com.programm.plugz.webserv.content.IContentWriter;
import com.programm.plugz.webserv.ex.WebservException;
import lombok.RequiredArgsConstructor;

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

@RequiredArgsConstructor
@Logger("Webserv")
class Webserver {

    private final ILogger log;
    private final ContentHandler contentHandler;

    private final Map<RequestType, Map<String, List<RequestMethodConfig>>> mappings = new HashMap<>();

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
            RequestInfo requestInfo =  parseRequestInfo(in);
            if(logRequests) log.info("[%7<({})]: {}", requestInfo.type, requestInfo.fullQuery);
            handleRequest(in, out, requestInfo);
        }
    }

    private RequestInfo parseRequestInfo(BufferedReader in) throws IOException {
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

    private void handleRequest(BufferedReader in, PrintWriter out, RequestInfo info) throws IOException {
        if(info.type == RequestType.OPTIONS){
            String requestingMethod = info.headers.get("Access-Control-Request-Method");

            if(requestingMethod.equals("POST")){
                if(invalidMapping(RequestType.POST, info.query)){
                    if(logRequests) log.info("[%7<({})]: {} was rejected: No request mapper found!", info.type, info.fullQuery);
                    replyError(out, "501 Not Implemented");
                    out.close();
                    return;
                }
            }
            else if(requestingMethod.equals("GET")){
                if(invalidMapping(RequestType.GET, info.query)){
                    if(logRequests) log.info("[%7<({})]: {} was rejected: No request mapper found!", info.type, info.fullQuery);
                    replyError(out, "501 Not Implemented");
                    out.close();
                    return;
                }
            }
            else {
                throw new IllegalStateException("NOT IMPLEMENTED: [" + requestingMethod + "]!");
            }

            replyOkOptions(out);
            out.close();
        }
        else {
            try {
                doMapping(out, in, info);
            } catch (WebservException e) {
                log.logException("Failed to do mapping [" + info.fullQuery + "]", e);
                replyError(out, "500 Internal Server Error");
            }

            out.close();
        }
    }

    private void doMapping(PrintWriter out, BufferedReader in, RequestInfo info) throws WebservException, IOException {
        List<RequestMethodConfig> configs = getConfigs(info.type, info.query);

        if(configs == null){
            if(logRequests) log.info("[%7<({})]: {} was rejected: No request mapper found!", info.type, info.fullQuery);
            replyError(out, "501 Not Implemented");
            return;
        }

        for(RequestMethodConfig config : configs){
            doMethodMapping(out, in, info, config);
        }
    }

    private void doMethodMapping(PrintWriter out, BufferedReader in, RequestInfo info, RequestMethodConfig config) throws WebservException, IOException {
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
                    for (int o=0;o<cLength;o++) {
                        char c = (char) in.read();
                        contentBuilder.append(c);
                    }
                }
                catch (SocketTimeoutException e){
                    log.error("Could not read full content length. Probably 'Umlaute' are the problem! [{}] characters less.", (cLength - contentBuilder.length()));
                }

                String contentType = info.headers.get("Content-Type");

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
            data = config.method.invoke(params);
        }
        catch (MagicInstanceException e){
            throw new WebservException("Exception calling request method for [" + info.fullQuery + "]!", e);
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
        out.print("Accept: application/json;charset=UTF-8\r\n");
        out.print("Accept-Charset: UTF-8\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("Access-Control-Allow-Headers: Content-Type\r\n");
        out.print("\r\n"); // End of headers
    }

}
