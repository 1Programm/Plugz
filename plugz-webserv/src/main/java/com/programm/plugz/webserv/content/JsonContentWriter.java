package com.programm.plugz.webserv.content;

import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.files.json.JsonNode;
import com.programm.plugz.object.mapper.ISpecializedObjectMapperLookup;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.object.mapper.property.PropertyObjectJsonNodeMapper;

class JsonContentWriter implements IContentWriter {

    private final PropertyObjectJsonNodeMapper writer;

    public JsonContentWriter(ClassAnalyzer analyzer, ISpecializedObjectMapperLookup specializedLookup) {
        this.writer = new PropertyObjectJsonNodeMapper(analyzer, specializedLookup, true);
    }

    @Override
    public String write(Object object) throws ObjectMapException {
        JsonNode node = writer.read(object);
        return node.toString();
    }
}
