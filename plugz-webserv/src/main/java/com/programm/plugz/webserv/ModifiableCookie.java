package com.programm.plugz.webserv;

import java.util.Date;

public class ModifiableCookie extends Cookie {

    ModifiableCookie(String name, String value) {
        super(name, value);
    }

    public ModifiableCookie expires(Date expires){
        this.expires = expires;
        return this;
    }

    public ModifiableCookie maxAge(Integer maxAge){
        this.maxAge = maxAge;
        return this;
    }

    public ModifiableCookie domain(String domain) {
        this.domain = domain;
        return this;
    }

    public ModifiableCookie path(String path){
        this.path = path;
        return this;
    }

    public ModifiableCookie secure(boolean secure){
        this.secure = secure;
        return this;
    }

    public ModifiableCookie httpOnly(boolean httpOnly){
        this.httpOnly = httpOnly;
        return this;
    }

    public ModifiableCookie sameSite(String sameSite){
        this.sameSite = sameSite;
        return this;
    }
}
