package com.programm.plugz.test;

import com.programm.plugz.webserv.api.GetMapping;
import com.programm.plugz.webserv.api.RestController;

@RestController("/index")
public class TestIndexPageController {

    @GetMapping(value = "1", contentType = "text/html")
    public String getIndexView1(){
        return "<h1>Hello World!</h1>\n<p>Test paragraph</p>\n<button onclick=\"console.log('hi')\">Test Button</button>";
    }

    @GetMapping(value = "2", contentType = "text/html")
    public String getIndexView2(){
        return "<a href=\"/index/1\">Click me :D</a>";
    }

}
