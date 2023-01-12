package com.programm.plugz.files.props;

import com.programm.plugz.files.xml.XmlBuilder;
import com.programm.plugz.files.xml.XmlNode;
import com.programm.plugz.files.xml.XmlParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PropsBuilder {

    public static PropsNode fromResource(String name) throws IOException, PropsParseException {
        InputStream is = XmlBuilder.class.getResourceAsStream(name);
        if(is == null) throw new IOException("No resource named [" + name + "] found!");
        return fromInputStream(is);
    }

    public static PropsNode fromFile(File file) throws IOException, PropsParseException {
        try(FileInputStream fis = new FileInputStream(file)){
            return fromInputStream(fis);
        }
    }

    public static PropsNode fromInputStream(InputStream is) throws IOException, PropsParseException {
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

    public static PropsNode fromString(String s) throws PropsParseException {
        List<PropsNode> nodes = new ArrayList<>();
        int last = 0;
        String key = null;

        for(int i=0;i<s.length();i++){
            if(s.charAt(i) == '='){
                if(key != null) continue;
                key = s.substring(last, i).trim();
                last = i + 1;
            }
            else if(s.charAt(i) == '\n'){
                if(key == null) continue;
                String value = s.substring(last, i).trim();
                //TODO: Check for changes this implies
                //if(value.isBlank()) throw new PropsParseException("No key value pair defined!");
                nodes.add(new PropsKeyValNode(key, value));
                key = null;
                last = i + 1;
            }
        }

        if(last < s.length()){
            String value = s.substring(last).trim();
            nodes.add(new PropsKeyValNode(key, value));
        }

        return new PropsRootNode(nodes);
    }


    public static String toString(PropsNode node) {
        if(node instanceof PropsKeyValNode){
            return node.name() + " = " + node.value();
        }

        StringBuilder sb = new StringBuilder();

        for(PropsNode child : node.children()){
            if(sb.length() != 0) sb.append("\n");
            sb.append(child.name()).append(" = ").append(child.value());
        }

        return sb.toString();
    }

}
