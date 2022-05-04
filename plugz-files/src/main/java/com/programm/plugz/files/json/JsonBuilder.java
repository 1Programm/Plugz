package com.programm.plugz.files.json;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JsonBuilder {

    public static JsonNode fromResource(String name) throws IOException, JsonParseException {
        InputStream is = JsonBuilder.class.getResourceAsStream(name);
        if(is == null) throw new IOException("No resource named [" + name + "] found!");
        return fromInputStream(is);
    }

    public static JsonNode fromFile(File file) throws IOException, JsonParseException {
        try(FileInputStream fis = new FileInputStream(file)){
            return fromInputStream(fis);
        }
    }

    public static JsonNode fromInputStream(InputStream is) throws IOException, JsonParseException {
        StringBuilder sb = new StringBuilder();

        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))){
            String line;
            while((line = br.readLine()) != null){
                if(sb.length() != 0){
                    sb.append("\n");
                }
                sb.append(line);
            }
        }

        return fromString(sb.toString());
    }

    public static JsonNode fromString(String s) throws JsonParseException {
        return getValue(s, new AtomicInteger());
    }

    private static JsonNode getValue(String s, AtomicInteger pos) throws JsonParseException {
        int sLen = s.length();

        advanceWhitespace(s, pos, sLen);
        if(pos.get() == sLen) throw new JsonParseException("Empty value!");

        char curChar = s.charAt(pos.get());

        if(Character.isDigit(curChar) || (curChar == '-' && Character.isDigit(s.charAt(pos.get() + 1)))){
            int p = pos.get() + 1;
            while(p < sLen){
                curChar = s.charAt(p);
                if(curChar != '.' && !Character.isDigit(curChar)) break;
                p++;
            }

            String _num = s.substring(pos.get(), p);
            pos.set(p);
            try {
                double num = Double.parseDouble(_num);
                return new JsonValueNode(num);
            }
            catch (NumberFormatException e){
                throw new JsonParseException("Invalid number: [" + _num + "]!");
            }
        }
        else if(curChar == '"'){
            String text = loadString(s, pos, sLen);
            return new JsonValueNode(text);
        }
        else if(curChar == '['){
            pos.incrementAndGet();
            List<JsonNode> children = loadArray(s, pos, sLen);
            return new JsonArrayNode(children);
        }
        else if(curChar == '{'){
            pos.incrementAndGet();
            Map<String, JsonNode> children = loadObject(s, pos, sLen);
            return new JsonObjectNode(children);
        }
        else if(s.startsWith("true", pos.get())){
            pos.addAndGet(4);
            return new JsonValueNode(true);
        }
        else if(s.startsWith("false", pos.get())){
            pos.addAndGet(5);
            return new JsonValueNode(false);
        }
        else {
            throw new JsonParseException("Invalid begin of value: [" + curChar + "]!");
        }
    }

    private static String loadString(String s, AtomicInteger pos, int end) throws JsonParseException {
        int p = pos.get() + 1;
        while(p < end){
            char curChar = s.charAt(p);
            if(curChar == '"' && s.charAt(p - 1) != '\\') break;
            if(p + 1 == end) throw new JsonParseException("String does not end with a quotation!");
            p++;
        }

        String text = s.substring(pos.get() + 1, p);
        pos.set(p + 1);
        return text;
    }

    private static List<JsonNode> loadArray(String s, AtomicInteger pos, int end) throws JsonParseException {
        List<JsonNode> nodes = new ArrayList<>();

        advanceWhitespace(s, pos, end);
        if(s.charAt(pos.get()) == ']') return nodes;

        while(pos.get() < end){
            JsonNode node = getValue(s, pos);
            nodes.add(node);

            advanceWhitespace(s, pos, end);
            char curChar = s.charAt(pos.get());
            if(curChar == ']') break;
            if(curChar != ',') throw new JsonParseException("Array values must be separated by a comma!");
            pos.incrementAndGet();

            if(pos.get() == end) throw new JsonParseException("Array is not closed!");
        }

        pos.incrementAndGet();
        return nodes;
    }

    private static Map<String, JsonNode> loadObject(String s, AtomicInteger pos, int end) throws JsonParseException {
        Map<String, JsonNode> nodes = new HashMap<>();

        while(pos.get() < end){
            advanceWhitespace(s, pos, end);
            char curChar = s.charAt(pos.get());
            if(curChar == '}') return nodes;
            if(s.charAt(pos.get()) != '"') throw new JsonParseException("Objects must start with a key - string!");
            String key = loadString(s, pos, end);

            advanceWhitespace(s, pos, end);
            if(s.charAt(pos.get()) != ':') throw new JsonParseException("Obejct must be specified in key - value pairs separated by a colon!");
            pos.incrementAndGet();


            JsonNode node = getValue(s, pos);
            nodes.put(key, node);

            advanceWhitespace(s, pos, end);

            if(pos.get() == end) throw new JsonParseException("Object is not closed!");

            curChar = s.charAt(pos.get());
            if(curChar == '}') break;
            if(curChar != ',') throw new JsonParseException("Objects key-value pairs must be separated by a comma!");
            pos.incrementAndGet();

            if(pos.get() == end) throw new JsonParseException("Object is not closed!");
        }

        pos.incrementAndGet();
        return nodes;
    }

    private static void advanceWhitespace(String s, AtomicInteger pos, int end){
        while(pos.get() < end && Character.isWhitespace(s.charAt(pos.get()))){
            pos.incrementAndGet();
        }
    }

    public static String toString(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        buildString(node, sb);
        return sb.toString();
    }

    private static void buildString(JsonNode node, StringBuilder sb){
        if(node instanceof JsonValueNode valueNode){
            Object value = valueNode.get();
            if(value instanceof String){
                sb.append("\"").append(value).append("\"");
            }
            else {
                sb.append(value);
            }
        }
        else if(node instanceof JsonArrayNode arrayNode){
            sb.append("[");
            for(int i=0;i<arrayNode.size();i++){
                if(i != 0) sb.append(",");
                buildString(arrayNode.get(i), sb);
            }
            sb.append("]");
        }
        else if(node instanceof JsonObjectNode objectNode){
            sb.append("{");
            boolean init = true;
            for(Map.Entry<String, JsonNode> entry : objectNode.objectChildren().entrySet()){
                if(init) init = false;
                else sb.append(",");

                sb.append("\"").append(entry.getKey()).append("\":");
                buildString(entry.getValue(), sb);
            }
            sb.append("}");
        }
    }

}
