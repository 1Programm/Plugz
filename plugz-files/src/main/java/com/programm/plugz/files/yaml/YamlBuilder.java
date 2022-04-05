package com.programm.plugz.files.yaml;

import java.io.*;
import java.util.List;

public class YamlBuilder {

    public static YamlNode fromResource(String name) throws IOException, YamlParseException {
        InputStream is = YamlBuilder.class.getResourceAsStream(name);
        if(is == null) throw new IOException("No resource named [" + name + "] found!");
        return fromInputStream(is);
    }

    public static YamlNode fromFile(File file) throws IOException, YamlParseException {
        try(FileInputStream fis = new FileInputStream(file)){
            return fromInputStream(fis);
        }
    }

    public static YamlNode fromInputStream(InputStream is) throws IOException, YamlParseException {
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

    public static YamlNode fromString(String s) throws YamlParseException {
        List<YamlNode> nodes = loadChildren(s);

        return nodes.get(0);
    }

    private static List<YamlNode> loadChildren(String s) throws YamlParseException {
        return null;//TODO
    }

    public static String toString(YamlNode node){
        StringBuilder sb = new StringBuilder();
        buildString(node, sb, 0);
        return sb.toString();
    }

    private static void buildString(YamlNode node, StringBuilder sb, int tabs){
        //TODO
    }

}
