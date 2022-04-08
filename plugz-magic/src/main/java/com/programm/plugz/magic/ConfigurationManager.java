package com.programm.plugz.magic;

import com.programm.plugz.api.MagicSetupException;
import com.programm.plugz.api.utils.ValueUtils;
import com.programm.plugz.files.ResourceNode;
import com.programm.plugz.files.props.PropsBuilder;
import com.programm.plugz.files.props.PropsNode;
import com.programm.plugz.files.props.PropsParseException;
import com.programm.plugz.files.xml.XmlBuilder;
import com.programm.plugz.files.xml.XmlNode;
import com.programm.plugz.files.xml.XmlParseException;
import com.programm.plugz.files.yaml.YamlBuilder;
import com.programm.plugz.files.yaml.YamlNode;
import com.programm.plugz.files.yaml.YamlParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

class ConfigurationManager {

    public final Map<String, Object> configValues = new HashMap<>();
    public String configProfile;

    public void init(String... args) throws MagicSetupException {
        Map<String, Object> tmpConfigArgs = collectConfigArgs(args);

        readProfile();

        configValues.putAll(tmpConfigArgs);
    }

    public void registerConfiguration(String key, Object value){
        configValues.put(key, value);
    }

    private Map<String, Object> collectConfigArgs(String... args) throws MagicSetupException {
        Map<String, Object> tmpConfigArgs = new HashMap<>();

        for(int i=0;i<args.length;i++){
            if(args[i].startsWith("-")){
                String key = args[i].substring(1);

                if(key.equals("config-profile")){
                    if(i + 1 == args.length) throw new MagicSetupException("Config profile value not defined in args!");
                    configProfile = args[i + 1];
                    continue;
                }

                Object value;
                if(i + 1 == args.length){
                    value =  true;
                }
                else {
                    value = getPrimitiveConfigValue(args[i + 1]);
                }

                tmpConfigArgs.put(key, value);
            }
        }

        return tmpConfigArgs;
    }

    private Object getPrimitiveConfigValue(String s){
        Object value = ValueUtils.getPrimitiveValue(s);

        if(value == s){
            try{ return Class.forName(s); } catch (ClassNotFoundException ignore){}
        }

        return value;
    }

    private void readProfile() throws MagicSetupException {
        if(configProfile == null) {
            tryLoadConfigResource(true, "" +
                    "plugz.xml",
                    "plugz.yml",
                    "plugz.properties"
            );
        }
        else {
            tryLoadConfigResource(false, "" +
                    "plugz-" + configProfile + ".xml",
                    "plugz-" + configProfile + ".yml",
                    "plugz-" + configProfile + ".properties"
            );
        }
    }

    private void tryLoadConfigResource(boolean allowFail, String... names) throws MagicSetupException {
        for(String name : names){
            InputStream is = ConfigurationManager.class.getResourceAsStream("/" + name);
            if(is == null) continue;

            try{
                if(name.endsWith(".xml")){
                    loadXmlConfigResource(is);
                }
                else if(name.endsWith(".yml") || name.endsWith(".yaml")){
                    loadYamlConfigResource(is);
                }
                else if(name.endsWith(".properties")){
                    loadPropsConfigResource(is);
                }
                else {
                    throw new MagicSetupException("Invalid resource file-type [" + name + "]!");
                }

                return;
            }
            catch (IOException e){
                throw new MagicSetupException("Failed to read the config file [" + name + "]!", e);
            }
            catch (XmlParseException e){
                throw new MagicSetupException("Failed to parse xml from config file [" + name + "]!", e);
            }
            catch (YamlParseException e){
                throw new MagicSetupException("Failed to parse yaml from config file [" + name + "]!", e);
            }
            catch (PropsParseException e){
                throw new MagicSetupException("Failed to parse properties from config file [" + name + "]!", e);
            }
        }

        if(!allowFail) {
            throw new MagicSetupException("Could not find the configuration profile [" + configProfile + "]!");
        }
    }

    private void loadXmlConfigResource(InputStream is) throws IOException, XmlParseException {
        XmlNode rootNode = XmlBuilder.fromInputStream(is);
        if(!rootNode.name().equals("plugz")) throw new XmlParseException("Root node must be called [plugz]!");

        for(XmlNode child : rootNode.children()) {
            loadFromResourceNode(child, "");
        }
    }

    private void loadYamlConfigResource(InputStream is) throws IOException, YamlParseException {
        YamlNode rootNode = YamlBuilder.fromInputStream(is);

        for(YamlNode child : rootNode.children()) {
            loadFromResourceNode(child, "");
        }
    }

    private void loadPropsConfigResource(InputStream is) throws IOException, PropsParseException {
        PropsNode rootNode = PropsBuilder.fromInputStream(is);

        for(PropsNode child : rootNode.children()) {
            loadFromResourceNode(child, "");
        }
    }

    private void loadFromResourceNode(ResourceNode node, String curPath) {
        String name = node.name();
        String _value = node.value();

        String nPath = curPath;
        if(nPath.isEmpty()){
            nPath = name;
        }
        else {
            nPath += "." + name;
        }

        if(_value != null){
            Object value = getPrimitiveConfigValue(_value);
            configValues.put(nPath, value);
            return;
        }

        for(ResourceNode child : node.children()){
            loadFromResourceNode(child, nPath);
        }
    }

}