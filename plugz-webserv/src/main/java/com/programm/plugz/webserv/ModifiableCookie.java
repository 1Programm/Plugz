package com.programm.plugz.webserv;

import java.util.Date;

public class ModifiableCookie extends Cookie {

    ModifiableCookie(String name, String value) {
        super(name, value);
    }

    public ModifiableCookie setExpires(Date expires){
        this.expires = expires;
        return this;
    }

    public ModifiableCookie setMaxAge(Integer maxAge){
        this.maxAge = maxAge;
        return this;
    }

    public ModifiableCookie setDomain(String domain){
        this.domain = domain;
        return this;
    }

    public ModifiableCookie setPath(String path){
        this.path = path;
        return this;
    }

    public ModifiableCookie enableSecure(){
        this.secure = true;
        return this;
    }

    public ModifiableCookie enableHttpOnly(){
        this.httpOnly = true;
        return this;
    }

    public ModifiableCookie setSameSite(String sameSite){
        this.sameSite = sameSite;
        return this;
    }
}
