package com.programm.plugz.webserv.content;

import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.files.json.JsonBuilder;
import com.programm.plugz.files.json.JsonNode;
import com.programm.plugz.files.json.JsonParseException;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.object.mapper.property.JsonPropertyReader;

class JsonContentReader implements IContentReader {

    private final JsonPropertyReader reader;

    public JsonContentReader(ClassAnalyzer classAnalyzer) {
        this.reader = new JsonPropertyReader(classAnalyzer);
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

        return (T) reader.read(contentNode, cls);
    }

}
