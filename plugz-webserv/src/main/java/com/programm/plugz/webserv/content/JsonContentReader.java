package com.programm.plugz.webserv.content;

import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.files.json.JsonBuilder;
import com.programm.plugz.files.json.JsonNode;
import com.programm.plugz.files.json.JsonParseException;
import com.programm.plugz.object.mapper.ISpecializedObjectMapperLookup;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.object.mapper.property.JsonNodePropertyObjectMapper;

class JsonContentReader implements IContentReader {

    private final JsonNodePropertyObjectMapper reader;

    public JsonContentReader(ClassAnalyzer classAnalyzer, ISpecializedObjectMapperLookup specializedLookup) {
        this.reader = new JsonNodePropertyObjectMapper(classAnalyzer, specializedLookup);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(String content, Class<T> cls) throws ObjectMapException {
        JsonNode contentNode;
        try {
            contentNode = JsonBuilder.fromString(content);
        }
        catch (JsonParseException e){
            throw new ObjectMapException("Invalid json content [" + content + "]!", e);
        }

        return (T) reader._read(contentNode, cls);
    }

}
