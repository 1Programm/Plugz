package com.programm.plugz.webserv.content;

import com.programm.plugz.cls.analyzer.ClassAnalyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ContentHandler {

    private final Map<String, IContentReader> readers = new HashMap<>();
    private final Map<String, IContentWriter> writers = new HashMap<>();

    public ContentHandler(){
        ClassAnalyzer analyzer = new ClassAnalyzer(true);

        readers.put("application/x-www-form-urlencoded", new PlainTextContentReader());
        readers.put("application/json", new JsonContentReader(analyzer));

        writers.put("application/text", Objects::toString);
        writers.put("text/html", Objects::toString);
        writers.put("text/plain", Objects::toString);
        writers.put("application/json", new JsonContentWriter(analyzer));
    }

    public IContentReader getReader(String name) {
        return readers.get(name);
    }

    public IContentWriter getWriter(String name) {
        return writers.get(name);
    }

}
