package com.programm.plugz.webserv.content;

import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.files.json.JsonNode;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.object.mapper.property.JsonPropertyWriter;

class JsonContentWriter implements IContentWriter {

    private final JsonPropertyWriter writer;

    public JsonContentWriter(ClassAnalyzer analyzer) {
        this.writer = new JsonPropertyWriter(analyzer);
    }

    @Override
    public String write(Object object) throws ObjectMapException {
        JsonNode node = writer.write(object);
        return node.toString();
    }
}
