package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostSetup;
import com.programm.plugz.magic.MagicEnvironment;
import com.programm.plugz.webserv.Cookie;
import com.programm.plugz.webserv.api.config.RestConfig;

import java.util.Map;

@Config
public class Main {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @Get private ILogger log;

    @PostSetup
    public void configureRestConfig(@Get RestConfig config){
        config.fallbackInterceptor((handler, request) -> {
            String query = request.query();

            if(query.equals("/blass")){
                return request.doOk("text/html", "<h1>Special fallback</h1>");
            }

            return request.doOk("text/html", "<h1>Error fallback</h1>");
        });

        config.forPath("/test")
                .with(request -> {
                    log.info(request.cookies().toString());
                    return true;
                })
                .onSuccessOk();

        config.forPath("/bla")
                .onSuccess((handler, request) -> {
                    StringBuilder sb = new StringBuilder();
                    Map<String, Cookie> cookies = request.cookies();

                    for(Cookie cookie : cookies.values()){
                        if(sb.length() == 0) sb.append("<ul>");
                        sb.append("<li>").append(cookie).append("</li>");
                    }

                    if(sb.length() != 0) sb.append("</ul>");

                    return request.doOk("text/html", sb.toString());
                });

        config.forPath("/cookietest")
                .onSuccess((handler, req) -> {
                    return req
                            .setCookie(handler
                                    .buildCookie("othercookie", "lol"))
                            .doOk();
                });
    }

}
