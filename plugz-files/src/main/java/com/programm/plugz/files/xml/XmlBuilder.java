package com.programm.plugz.files.xml;

import com.programm.plugz.files.StringUtils;

import java.io.*;
import java.util.*;

public class XmlBuilder {

    public static XmlNode fromResource(String name) throws IOException, XmlParseException {
        InputStream is = XmlBuilder.class.getResourceAsStream(name);
        if(is == null) throw new IOException("No resource named [" + name + "] found!");
        return fromInputStream(is);
    }

    public static XmlNode fromFile(File file) throws IOException, XmlParseException {
        try(FileInputStream fis = new FileInputStream(file)){
            return fromInputStream(fis);
        }
    }

    public static XmlNode fromInputStream(InputStream is) throws IOException, XmlParseException {
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

    public static XmlNode fromString(String s) throws XmlParseException {
        List<XmlNode> nodes = loadChildren(s);

        if(nodes.isEmpty()){
            throw new XmlParseException("Empty file!");
        }
        else if(nodes.size() > 1){
            throw new XmlParseException("Invalid xml: There are multiple root nodes!");
        }

        return nodes.get(0);
    }

    private static List<XmlNode> loadChildren(String s) throws XmlParseException {
        List<XmlNode> nodes = new ArrayList<>();

        int index = 0;
        int end = s.length();

        Stack<XmlNode> toBeClosedStack = new Stack<>();
        Stack<List<XmlNode>> toBeClosedChildrenStack = new Stack<>();

        List<XmlNode> curChildren = nodes;

        while(index < end) {
            int nextOpen = s.indexOf('<', index);
            if(nextOpen == -1 || nextOpen > end) break;

            String before = s.substring(index, nextOpen).trim();
            if(!before.isEmpty()){
                curChildren.add(new XmlTextNode(before));
            }

            if(nextOpen + 1 < end && s.charAt(nextOpen + 1) == '/'){
                int nextChar = StringUtils.trimIndexForward(s, nextOpen + 2, end);
                int endClosingTagName = nextChar;
                int endClosingTag = -1;
                while(true){
                    if(s.charAt(endClosingTagName) == '>'){
                        endClosingTag = endClosingTagName;
                        break;
                    }
                    else if(Character.isWhitespace(s.charAt(endClosingTagName))){
                        break;
                    }

                    endClosingTagName++;
                }

                if(endClosingTag == -1){
                    endClosingTag = endClosingTagName + 1;
                    while(Character.isWhitespace(s.charAt(endClosingTag))){
                        endClosingTag++;
                    }

                    if(s.charAt(endClosingTag) != '>') throw new XmlParseException("Invalid end for closing tag!");
                }

                String nodeName = s.substring(nextChar, endClosingTagName);
                index = endClosingTag + 1;

                if(toBeClosedStack.isEmpty()) throw new XmlParseException("Invalid closing tag for [" + nodeName + "] found!");
                XmlNode parent = toBeClosedStack.pop();

                if(!parent.name().equals(nodeName)) throw new XmlParseException("Unexpected closing tag found: [" + nodeName + "] expected closing tag for [" + parent.name() + "]!");

                curChildren = toBeClosedChildrenStack.pop();
                continue;
            }
            else if(nextOpen + 3 < end && s.startsWith("<!--", nextOpen)){
                int nextCommentClose = s.indexOf("-->", nextOpen);
                if(nextCommentClose != -1){
                    index = nextCommentClose + 3;
                }
                else {
                    throw new XmlParseException("Comment was never closed!");
                }

                continue;
            }

            int nextClose = StringUtils.findNextOutsideComment(s, nextOpen + 1, end, ">", "\"", "\"", false);
            if(nextClose == -1 || nextClose > end) throw new XmlParseException("No closing '>' character found for opening node-tag.");

            int attribsStart = StringUtils.trimIndexForward(s, nextOpen + 1, nextClose);
            int attribsEnd = StringUtils.trimIndexBackward(s, nextOpen + 1, nextClose);
            String[] attribSplit = StringUtils.splitOutsideComment(s, attribsStart, attribsEnd, " ", "\"", "\"", false);
            String nodeName = attribSplit[0];

            List<XmlNode> children = new ArrayList<>();
            XmlNode node = new XmlGroupNode(nodeName, parseAttribs(attribSplit), children);

            toBeClosedChildrenStack.add(curChildren);
            curChildren.add(node);
            curChildren = children;
            toBeClosedStack.add(node);

            index = nextClose + 1;
        }

        return nodes;
    }

    private static Map<String, String> parseAttribs(String[] attribs) throws XmlParseException {
        Map<String, String> map = new HashMap<>();

        for(int i=1;i<attribs.length;i++){
            String attrib = attribs[i].trim();
            int nextEqual = attrib.indexOf('=');

            String key, value;
            if(nextEqual == -1) {
                key = attrib;
                value = "";
            }
            else {
                key = attrib.substring(0, nextEqual);

                if(attrib.charAt(nextEqual + 1) != '"' || attrib.charAt(attrib.length() - 1) != '"') throw new XmlParseException("Attribute value is not in quotations!");

                value = attrib.substring(nextEqual + 2, attrib.length() - 1);
            }

            map.put(key, value);
        }

        return map;
    }

    public static String toString(XmlNode node) {
        StringBuilder sb = new StringBuilder();
        buildString(node, sb, 0);
        return sb.toString();
    }

    private static void buildString(XmlNode node, StringBuilder sb, int tabs){
        if(node instanceof XmlTextNode){
            sb.append(node.value());
            return;
        }

        XmlGroupNode groupNode = (XmlGroupNode) node;

        String _tabs = "\t".repeat(tabs);
        sb.append(_tabs).append("<").append(groupNode.name());
        addAttribs(sb, groupNode.attribs());
        sb.append(">");

        if(groupNode.children.size() == 1 && groupNode.children.get(0) instanceof XmlTextNode){
            sb.append(groupNode.children.get(0).value());
        }
        else {
            for (XmlNode child : groupNode.children()) {
                sb.append("\n");
                buildString(child, sb, tabs + 1);
            }

            sb.append("\n").append(_tabs);
        }

        sb.append("</").append(groupNode.name()).append(">");
    }

    private static void addAttribs(StringBuilder sb, Map<String, String> attribs){
        for(Map.Entry<String, String> entry : attribs.entrySet()){
            sb.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
    }

}
