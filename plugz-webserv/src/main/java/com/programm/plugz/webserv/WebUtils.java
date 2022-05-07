package com.programm.plugz.webserv;

class WebUtils {

    public static String concatPathMapping(String a, String b){
        String path;

        if(a.isEmpty()) {
            path = b.isEmpty() ? "/" : b;
        }
        else if(b.isEmpty()) {
            path = a;
        }
        else if(a.endsWith("/") && b.startsWith("/")){
            path = a + b.substring(1);
        }
        else if(a.endsWith("/") || b.startsWith("/")){
            path = a + b;
        }
        else {
            path = a + "/" + b;
        }

        return path;
    }

}
