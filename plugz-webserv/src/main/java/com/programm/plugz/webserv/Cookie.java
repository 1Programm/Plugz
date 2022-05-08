package com.programm.plugz.webserv;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Cookie {
    final String name;
    final String value;
    Date expires;
    Integer maxAge;
    String domain;
    String path;
    boolean secure;
    boolean httpOnly;
    String sameSite;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name(){
        return name;
    }

    public String value(){
        return value;
    }

    public Date expires(){
        return expires;
    }

    public Integer maxAge() {
        return maxAge;
    }

    public String domain() {
        return domain;
    }

    public String path() {
        return path;
    }

    public boolean secure() {
        return secure;
    }

    public boolean httpOnly() {
        return httpOnly;
    }

    public String sameSite() {
        return sameSite;
    }

    public String toString()        {
        StringBuilder sb = new StringBuilder();

        sb.append(name).append("=").append(value);

        if (expires != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
            sb.append("; Expires=").append(fmt.format(expires)).append(" GMT");
        }

        if(maxAge != null) {
            sb.append("; Max-Age=").append(maxAge);
        }

        if(domain != null) {
            sb.append("; Domain=").append(domain);
        }

        if(path != null) {
            sb.append("; Path=").append(path);
        }

        if(secure) {
            sb.append("; Secure");
        }

        if(httpOnly) {
            sb.append("; HttpOnly");
        }

        if(sameSite != null) {
            sb.append("; SameSite=").append(sameSite);
        }

        return sb.toString();
    }

}
