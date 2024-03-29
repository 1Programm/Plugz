package com.programm.plugz.magic;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.api.MagicSetupException;
import com.programm.plugz.api.PlugzConfig;
import com.programm.plugz.api.utils.ValueParseException;
import com.programm.plugz.api.utils.ValueUtils;
import com.programm.plugz.files.NamedResourceNode;
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

@Logger("Config Manager")
class ConfigurationManager implements PlugzConfig {

    private final ILogger log;
    private final String[] args;

    public final Map<String, Object> configValues = new HashMap<>();
    public String configProfile;

    public ConfigurationManager(ILogger log, String... args) {
        this.log = log;
        this.args = args;
    }

    public void initProfileConfig() throws MagicSetupException {
        readProfileNameFromArgs();
        readProfile();
    }

    @Override
    public void registerDefaultConfiguration(String key, Object value){
        if(configValues.containsKey(key)) return;
        log.trace("Default Config: %30<({}) -> {}", key, value);
        configValues.put(key, value);
    }

    @Override
    public void registerConfiguration(String key, Object value){
        log.trace("Config: %30<({}) -> {}", key, value);
        configValues.put(key, value);
    }

    @Override
    public String profile() {
        return configProfile;
    }

    @Override
    public <T> T get(String name, Class<T> cls) throws ValueParseException {
        Object val = configValues.get(name);
        if(val == null) return null;

        return ValueUtils.parsePrimitive(val, cls);
    }

    public void initArgs() {
        log.trace("Reading configs from program args...");
        int numConfigs = 0;
        for(int i=0;i<args.length;i++){
            if(args[i].startsWith("-")){
                String key = args[i].substring(1);

//                if(key.equals("config.profile")){
//                    if(i + 1 < args.length) {
//                        configProfile = args[i + 1];
//                        numConfigs++;
//                    }
//                    continue;
//                }

                Object value;
                if(i + 1 == args.length){
                    value =  true;
                }
                else {
                    value = ValueUtils.getPrimitiveValue(args[i + 1]);
                }

                log.trace("# %30<({}) -> {}", key, value);
                configValues.put(key, value);
                numConfigs++;
            }
        }

        log.trace("Registered [{}] configs from program args.", numConfigs);
    }

    private void readProfileNameFromArgs() throws MagicSetupException{
        for(int i=0;i<args.length;i++){
            if(args[i].equals("-config.profile")){
                if(i + 1 == args.length) throw new MagicSetupException("No value provided for profile name inside arguments!");
                configProfile = args[i+1];
                log.debug("Found profile name: [{}] from arguments.", configProfile);
                return;
            }
        }
    }

    private void readProfile() throws MagicSetupException {
        log.debug("Searching config file for profile [{}]", configProfile);
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
            log.trace("Try to get config resource [{}]...", name);
            InputStream is = ConfigurationManager.class.getResourceAsStream("/" + name);
            if(is == null) continue;

            try{
                if(name.endsWith(".xml")){
                    log.trace("Reading configs from xml - file...");
                    loadXmlConfigResource(is);
                }
                else if(name.endsWith(".yml") || name.endsWith(".yaml")){
                    log.trace("Reading configs from yml/yaml - file...");
                    loadYamlConfigResource(is);
                }
                else if(name.endsWith(".properties")){
                    log.trace("Reading configs from properties - file...");
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

        if(allowFail){
            log.trace("No default config file found");
        }
        else {
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

    private void loadFromResourceNode(NamedResourceNode node, String curPath) {
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
            Object value = ValueUtils.getPrimitiveValue(_value);
            log.trace("# %30<({}) -> {}", nPath, value);
            configValues.put(nPath, value);
            return;
        }

        for(NamedResourceNode child : node.children()){
            loadFromResourceNode(child, nPath);
        }
    }

}
