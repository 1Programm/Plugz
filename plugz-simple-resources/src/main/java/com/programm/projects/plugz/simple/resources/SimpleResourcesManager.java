package com.programm.projects.plugz.simple.resources;

import com.programm.projects.ioutils.file.types.TypeParseException;
import com.programm.projects.ioutils.file.types.props.Props;
import com.programm.projects.ioutils.file.types.props.PropsBuilder;
import com.programm.projects.ioutils.file.types.xml.XmlBuilder;
import com.programm.projects.ioutils.file.types.xml.XmlNode;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.resources.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

@Logger("Simple-Resources-Manager")
public class SimpleResourcesManager implements IResourcesManager {

    private interface NameFallback {
        String parse(String name);
    }

    private static final NameFallback[] NAME_FALLBACK_TRIES = new NameFallback[]{
            String::toUpperCase,
            Utils::camelCaseToDots,
            Utils::camelCaseToDotsUpper,
            Utils::camelCaseToUnderscore,
            Utils::camelCaseToUnderscoreUpper,
            Utils::underscoreToDots,
            Utils::underscoreToDotsUpper,
    };

    private static abstract class AbstractResult implements IResourceLoader.Result {
        final Object[] values;

        public AbstractResult(Object[] values) {
            this.values = values;
        }

        @Override
        public int size() {
            return values.length;
        }

        @Override
        public Object get(int i) {
            return values[i];
        }
    }

    private static class ResourceEntry {
        final Object instance;
        final String name;
        final List<TypeEntry> resourceTypes;
        final IResourceLoader.Result result;

        public ResourceEntry(Object instance, String name, List<TypeEntry> resourceTypes, IResourceLoader.Result result) {
            this.instance = instance;
            this.name = name;
            this.resourceTypes = resourceTypes;
            this.result = result;
        }
    }

    private static class MergedResourceEntry {
        final Object instance;
        final String[] names;
        final List<TypeEntry> resourceTypes;
        final IResourceLoader.Result[] results;
        final int[] onCloseStates;

        public MergedResourceEntry(Object instance, String[] names, List<TypeEntry> resourceTypes, IResourceLoader.Result[] results, int[] onCloseStates) {
            this.instance = instance;
            this.names = names;
            this.resourceTypes = resourceTypes;
            this.results = results;
            this.onCloseStates = onCloseStates;
        }
    }

    private static class TypeEntry implements IResourceLoader.Entry {
        final Field field;
        final Class<?> type;
        final String name;
        final boolean isFinal;
        final boolean isPrivate;
        final IValueFallback fallback;

        public TypeEntry(Field field, Class<?> type, String name, boolean isFinal, boolean isPrivate, IValueFallback fallback) {
            this.field = field;
            this.type = type;
            this.name = name;
            this.isFinal = isFinal;
            this.isPrivate = isPrivate;
            this.fallback = fallback;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<?> type() {
            return type;
        }

        @Override
        public IValueFallback fallback() {
            return fallback;
        }
    }

    private final Map<URL, List<ResourceEntry>> saveOnExitMap = new HashMap<>();
    private final Map<URL, List<MergedResourceEntry>> saveOnExitMergedMap = new HashMap<>();

    @Get private ILogger log;
    @Get private IInstanceManager instanceManager;

    public SimpleResourcesManager() {}

    public SimpleResourcesManager(ILogger log, IInstanceManager instanceManager) {
        this.log = log;
        this.instanceManager = instanceManager;
    }

    @Override
    public void startup() {
        if(log == null) throw new IllegalStateException("Logger must be set!");
        if(instanceManager == null) throw new IllegalStateException("Instance manager must be set!");
    }

    @Override
    public void shutdown() {
        for(URL url : saveOnExitMap.keySet()) {
            List<ResourceEntry> entries = saveOnExitMap.get(url);

            log.debug("Saving [{}] resources from url [{}]...", entries.size(), url);
            saveEntries(entries);
        }

        for(URL url : saveOnExitMergedMap.keySet()) {
            List<MergedResourceEntry> entries = saveOnExitMergedMap.get(url);

            log.debug("Saving [{}] resources from url [{}]...", entries.size(), url);
            saveMergedEntries(entries);
        }
    }

    private void saveEntries(List<ResourceEntry> entries){
        for(int i=0;i<entries.size();i++){
            ResourceEntry entry = entries.get(i);

            log.trace("#{} saving [{}]...", i, entry.name);
            try {
                saveResource(entry);
            }
            catch (MagicResourceException e){
                log.error("Error while saving resource [{}]: {}", entry.name, e.getMessage());
            }
        }
    }

    private void saveMergedEntries(List<MergedResourceEntry> entries){
        for(int i=0;i<entries.size();i++){
            MergedResourceEntry entry = entries.get(i);

            log.trace("#{} saving merged files: {}...", i, Arrays.toString(entry.names));
            try {
                saveResource(entry);
            }
            catch (MagicResourceException e){
                log.error("Error while saving merged resources [{}]: {}", Arrays.toString(entry.names), e.getMessage());
            }
        }
    }

    @Override
    public void removeUrl(URL url) {
        List<ResourceEntry> entries = saveOnExitMap.remove(url);

        if(entries != null){
            log.debug("Saving [{}] removed resources from url [{}]...", entries.size(), url);
            saveEntries(entries);
        }

        List<MergedResourceEntry> mergedEntries = saveOnExitMergedMap.remove(url);

        if(mergedEntries != null){
            log.debug("Saving [{}] removed merged resources from url [{}]...", mergedEntries.size(), url);
            saveMergedEntries(mergedEntries);
        }
    }

    @Override
    public Object buildMergedResourceObject(Class<?> cls) throws MagicResourceException {
        Resources resourceMergedAnnotation = cls.getAnnotation(Resources.class);

        if(resourceMergedAnnotation == null) throw new IllegalStateException("INVALID STATE: Class: [" + cls.getName() + "] should be annotated with @Resources or multiple @Resource but was not.");

        URL url = Utils.getUrlFromClass(cls);

        List<TypeEntry> resourceTypes = collectTypeEntries(cls);
        Object[] sources = new Object[resourceTypes.size()];
        IResourceLoader.Result[] saveResults = new IResourceLoader.Result[resourceTypes.size()];
        String[] resultNames = new String[resourceTypes.size()];
        int[] onCloseStates = new int[resourceTypes.size()];

        IResourceMerger merger = null;
        Object[] mergedResourceFields = new Object[resourceTypes.size()];

        for(Resource resourceAnnotation : resourceMergedAnnotation.value()){
            Class<? extends IResourceMerger> resMergerClass = resourceAnnotation.merger();

            if(merger == null && resMergerClass == IResourceMerger.class){
                resMergerClass = DefaultResourceMerger.class;
            }

            if(resMergerClass != IResourceMerger.class){
                try {
                    merger = instanceManager.getInstance(resMergerClass);
                }
                catch (MagicInstanceException ignore){
                    try {
                        merger = instanceManager.instantiate(resMergerClass);
                    }
                    catch (MagicInstanceException e){
                        throw new MagicResourceException("Failed to magically create the resource merger class: [" + resMergerClass.getName() + "].", e);
                    }
                }
            }

            String resName = getResourceName(resourceAnnotation, cls);
            int resOnClose = resourceAnnotation.onexit();
            int resNotFound = resourceAnnotation.notfound();
            Class<? extends IResourceLoader> resLoader = resourceAnnotation.loader();

            Object[] sourceBack = new Object[1];
            IResourceLoader.Result result = getResourceFields(resName, resNotFound, resourceTypes, resLoader, sourceBack);

            if(result == null){
                continue;
            }


            for(int i=0;i<result.size();i++){
                Object origValue = mergedResourceFields[i];
                Object mergeValue = result.get(i);
                TypeEntry entry = resourceTypes.get(i);

                mergedResourceFields[i] = merger.mergeValues(entry.name, mergedResourceFields[i], mergeValue);

                if(mergedResourceFields[i] == mergeValue){
                    sources[i] = sourceBack[0];
                    saveResults[i] = result;
                    resultNames[i] = resName;
                    onCloseStates[i] = resOnClose;
                }
                else if(mergedResourceFields[i] != origValue){
                    sources[i] = null;
                    saveResults[i] = null;
                    resultNames[i] = null;
                    onCloseStates[i] = -1;
                }
            }
        }

        for(int i=0;i<resourceTypes.size();i++){
            if(mergedResourceFields[i] == null){
                TypeEntry entry = resourceTypes.get(i);

                Object fallbackValue = null;

                if(entry.fallback != null){
                    fallbackValue = entry.fallback.fallback(entry.type, entry.name, sources[i]);
                }

                if(fallbackValue == null) {
                    throw new MagicResourceException("No resource found for field [" + entry.name + "] and no fallback defined.");
                }

                mergedResourceFields[i] = fallbackValue;
            }
        }

        boolean[] flag = new boolean[1];
        Object instance = instantiateFromConstructor(cls, resourceTypes, mergedResourceFields, flag);
        MergedResourceEntry instanceEntry = new MergedResourceEntry(instance, resultNames, resourceTypes, saveResults, onCloseStates);

        if(!flag[0]) {
            initInstance(cls, instance, resourceTypes, mergedResourceFields);
        }

        for (int onCloseState : onCloseStates) {
            if (onCloseState == Resource.ONEXIT_SAVE) {
                saveOnExitMergedMap.computeIfAbsent(url, u -> new ArrayList<>()).add(instanceEntry);
                break;
            }
        }

        return instance;
    }

    @Override
    public Object buildResourceObject(Class<?> cls) throws MagicResourceException {
        Resource resourceAnnotation = cls.getAnnotation(Resource.class);

        if(resourceAnnotation == null) throw new IllegalStateException("INVALID STATE: Class: [" + cls.getName() + "] should be annotated with @Resource but was not.");

        String resName = getResourceName(resourceAnnotation, cls);
        int resOnClose = resourceAnnotation.onexit();
        int resNotFound = resourceAnnotation.notfound();
        Class<? extends IResourceLoader> resLoader = resourceAnnotation.loader();

        URL url = Utils.getUrlFromClass(cls);

        log.debug("Building resource class: [{}] for resource name [{}].", cls.getName(), resName);

        List<TypeEntry> resourceTypes = collectTypeEntries(cls);
        log.trace("Found [{}] resource fields that need to be set.", resourceTypes.size());

        Object[] sourceBack = new Object[1];
        IResourceLoader.Result result = getResourceFields(resName, resNotFound, resourceTypes, resLoader, sourceBack);

        if(result == null) {
            return null;
        }

        Object[] resourceFields = new Object[result.size()];
        for(int i=0;i<result.size();i++){
            Object value = result.get(i);
            if(value == null){
                TypeEntry entry = resourceTypes.get(i);

                Object fallbackValue = null;

                if(entry.fallback != null){
                    fallbackValue = entry.fallback.fallback(entry.type, entry.name, sourceBack[0]);
                }

                if(fallbackValue == null) {
                    throw new MagicResourceException("No resource found for field [" + entry.name + "] and no fallback defined.");
                }

                resourceFields[i] = fallbackValue;
            }
            else {
                resourceFields[i] = value;
            }
        }

        boolean[] flag = new boolean[1];
        Object instance = instantiateFromConstructor(cls, resourceTypes, resourceFields, flag);
        ResourceEntry instanceEntry = new ResourceEntry(instance, resName, resourceTypes, result);

        if(!flag[0]) {
            initInstance(cls, instance, resourceTypes, resourceFields);
        }

        if(resOnClose == Resource.ONEXIT_SAVE) {
            log.debug("Registering resource class: [{}] to save on shutdown.", cls.getName());
            saveOnExitMap.computeIfAbsent(url, u -> new ArrayList<>()).add(instanceEntry);
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    private IResourceLoader.Result getResourceFields(String resName, int resNotFound, List<TypeEntry> resourceTypes, Class<? extends IResourceLoader> resLoader, Object[] sourceBack) throws MagicResourceException{
        if(resLoader == IResourceLoader.class) {
            log.debug("Using default resource loader.");
            return loadResourceFields(resName, resNotFound, resourceTypes, sourceBack);
        }
        else {
            IResourceLoader loader;

            try {
                loader = instanceManager.getInstance(resLoader);
            }
            catch (MagicInstanceException ignore){
                try {
                    loader = instanceManager.instantiate(resLoader);
                }
                catch (MagicInstanceException e){
                    throw new MagicResourceException("Failed to magically create the resource loader class: [" + resLoader.getName() + "].", e);
                }
            }

            log.debug("Using custom resource loader: [{}].", resLoader.getName());
            return loader.loadFields(resName, resNotFound, (List<IResourceLoader.Entry>)(List<?>)resourceTypes);
        }
    }

    private List<TypeEntry> collectTypeEntries(Class<?> cls) throws MagicResourceException {
        List<TypeEntry> resourceTypes = new ArrayList<>();

        Field[] fields = cls.getDeclaredFields();
        for(Field field : fields){
            int mods = field.getModifiers();
            if(!Modifier.isStatic(mods)){
                String name = field.getName();
                IValueFallback fallback = null;

                Value valueAnnotation = field.getAnnotation(Value.class);
                if(valueAnnotation != null){
                    String aValue = valueAnnotation.value();
                    Class<? extends IValueFallback> aFallback = valueAnnotation.fallback();

                    if(!aValue.equals("")){
                        name = aValue;
                    }

                    if(aFallback != IValueFallback.class){
                        try {
                            fallback = instanceManager.getInstance(aFallback);
                        }
                        catch (MagicInstanceException ignore){
                            try {
                                fallback = instanceManager.instantiate(aFallback);
                            }
                            catch (MagicInstanceException e){
                                throw new MagicResourceException("Failed to magically create the fallback class: [" + aFallback.getName() + "].", e);
                            }
                        }
                    }
                }

                resourceTypes.add(new TypeEntry(field, field.getType(), name, Modifier.isFinal(mods), Modifier.isPrivate(mods), fallback));
            }
        }

        return resourceTypes;
    }

    private IResourceLoader.Result loadResourceFields(String name, int notFound, List<TypeEntry> types, Object[] sourceBack) throws MagicResourceException {
        InputStream is;

        if(name.startsWith("/")){
            is = SimpleResourcesManager.class.getResourceAsStream(name);
        }
        else {
            is = SimpleResourcesManager.class.getResourceAsStream("/" + name);
        }

        if(is != null){
            log.debug("Loading resource fields from a static resource [{}].", name);
            Object[] values = loadResourceFieldsFromInputStream(is, name, types, sourceBack);
            return new AbstractResult(values) {
                @Override
                public void save(String[] names, Object[] values) throws MagicResourceException {
                    throw new MagicResourceException("Cannot save the static resource [" + name + "].");
                }
            };
        }

        File file = new File(name);

        if(!file.exists()){
            switch (notFound){
                case Resource.NOTFOUND_ERROR:
                    throw new MagicResourceException("Resource [" + name + "] could not be found!");
                case Resource.NOTFOUND_IGNORE:
                    log.debug("Could not find resource [{}] but was ignored.", name);
                    return null;
                case Resource.NOTFOUND_CREATE:
                    try {
                        if(!file.createNewFile()){
                            throw new MagicResourceException("Could not create file: [" + name + "]!");
                        }
                    } catch (IOException e) {
                        throw new MagicResourceException("Could not create file: [" + name + "]!", e);
                    }
                    break;
            }
        }

        try(InputStream in = new FileInputStream(file)){
            log.debug("Loading resource fields from a file resource [{}].", name);
            Object[] values = loadResourceFieldsFromInputStream(in, name, types, sourceBack);
            return new AbstractResult(values) {
                @Override
                public void save(String[] names, Object[] values) throws MagicResourceException {
                    saveResource(file.getAbsolutePath(), names, values);
                }
            };
        }
        catch (IOException e){
            throw new MagicResourceException("Could not read from file: [" + name + "]!", e);
        }
    }

    private Object[] loadResourceFieldsFromInputStream(InputStream is, String name, List<TypeEntry> types, Object[] sourceBack) throws MagicResourceException {
        int lastDot = name.lastIndexOf('.');
        String fileType = name.substring(lastDot == -1 ? name.length() : lastDot + 1);
        try {
            if(fileType.equals("properties")) {
                Props props = PropsBuilder.fromInputStream(is);
                sourceBack[0] = props;
                return loadResourceFieldsFromProps(props, types);
            }
            else if(fileType.equals("xml")){
                XmlNode node = XmlBuilder.fromInputStream(is);
                sourceBack[0] = node;
                return loadResourceFieldsFromXmlNode(node, types);
            }
        }
        catch (IOException e){
            throw new MagicResourceException("Could not load resource [" + name + "] from " + fileType + " file.", e);
        }
        catch (TypeParseException e){
            throw new MagicResourceException("Failed to parse the resource [" + name + "]!", e);
        }

        throw new MagicResourceException("Invalid resource type: [" + fileType + "]!");
    }

    private Object[] loadResourceFieldsFromProps(Props props, List<TypeEntry> types) throws MagicResourceException {
        Object[] values = new Object[types.size()];

        for(int i=0;i<types.size();i++){
            TypeEntry entry = types.get(i);
            String name = entry.name;
            String _val = props.get(name);

            if(_val == null){
                for(NameFallback nameFallback : NAME_FALLBACK_TRIES){
                    String _name = nameFallback.parse(name);
                    _val = props.get(_name);

                    if(_val != null){
                        break;
                    }
                }
            }

            if(_val == null){
                continue;
            }

            Object val = serializeType(entry.type, _val);
            values[i] = val;
        }

        return values;
    }

    private Object[] loadResourceFieldsFromXmlNode(XmlNode node, List<TypeEntry> types) throws MagicResourceException {
        Object[] values = new Object[types.size()];

        for(int i=0;i<types.size();i++){
            TypeEntry entry = types.get(i);
            String name = entry.name;
//            String _val = getValueFromXmlNode(node, name);
            XmlNode _val = getValueFromXmlNode(node, name);

            if(_val == null){
                for(NameFallback nameFallback : NAME_FALLBACK_TRIES){
                    String _name = nameFallback.parse(name);
                    _val = getValueFromXmlNode(node, _name);

                    if(_val != null){
                        break;
                    }
                }
            }

            if(_val == null){
                continue;
            }

            Object val = serializeXmlType(entry.type, _val);
            values[i] = val;
        }

        return values;
    }

    private XmlNode getValueFromXmlNode(XmlNode node, String key){
        return node.get(key);
    }

    private Object serializeType(Class<?> type, String _val) throws MagicResourceException {
        if(CharSequence.class.isAssignableFrom(type)){
            return _val;
        }
        else if(Utils.isSameClass(type, Boolean.class)){
            return Boolean.parseBoolean(_val);
        }
        else if(Utils.isSameClass(type, Character.class)){
            if(_val.length() != 1){
                throw new MagicResourceException("Parsing exception: Expected a character, got: [" + _val + "].");
            }

            return _val.charAt(0);
        }
        else if(Utils.isSameClass(type, Byte.class)){
            try {
                return Byte.parseByte(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a byte, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Short.class)){
            try {
                return Short.parseShort(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a short, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Integer.class)){
            try {
                return Integer.parseInt(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected an int, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Long.class)){
            try {
                return Long.parseLong(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a long, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Float.class)){
            try {
                return Float.parseFloat(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a float, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Double.class)){
            try {
                return Double.parseDouble(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a double, got: [" + _val + "].");
            }
        }

        return _val;
    }

    private Object serializeXmlType(Class<?> type, XmlNode node) throws MagicResourceException{
        if(CharSequence.class.isAssignableFrom(type)){
            return node.value();
        }
        else if(Utils.isSameClass(type, Boolean.class)){
            String _val = node.value();
            return Boolean.parseBoolean(_val);
        }
        else if(Utils.isSameClass(type, Character.class)){
            String _val = node.value();
            if(_val.length() != 1){
                throw new MagicResourceException("Parsing exception: Expected a character, got: [" + _val + "].");
            }

            return _val.charAt(0);
        }
        else if(Utils.isSameClass(type, Byte.class)){
            String _val = node.value();
            try {
                return Byte.parseByte(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a byte, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Short.class)){
            String _val = node.value();
            try {
                return Short.parseShort(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a short, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Integer.class)){
            String _val = node.value();
            try {
                return Integer.parseInt(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected an int, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Long.class)){
            String _val = node.value();
            try {
                return Long.parseLong(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a long, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Float.class)){
            String _val = node.value();
            try {
                return Float.parseFloat(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a float, got: [" + _val + "].");
            }
        }
        else if(Utils.isSameClass(type, Double.class)){
            String _val = node.value();
            try {
                return Double.parseDouble(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a double, got: [" + _val + "].");
            }
        }
        else if(List.class.isAssignableFrom(type)){
            List<Object> list = new ArrayList<>();
            XmlNode childNode = node.get("0");
            int pos = 1;
            while(childNode != null){
                String _childType = childNode.attribute("type");
                Class<?> childType;

                try {
                    childType = Class.forName(_childType);
                }
                catch (ClassNotFoundException e){
                    throw new MagicResourceException("Invalid class definition: [" + _childType + "]!", e);
                }

                Object child = serializeXmlType(childType, childNode);
                list.add(child);

                childNode = node.get("" + (pos++));
            }

            return list;
        }

        return null;
    }

    private String deserializeType(Object val) {
        Class<?> type = val.getClass();

        if(type == Boolean.TYPE){
            return Boolean.toString((boolean)val);
        }
        else if(type == Character.TYPE){
            return Character.toString((char)val);
        }
        else if(type == Byte.TYPE){
            return Byte.toString((byte)val);
        }
        else if(type == Short.TYPE){
            return Short.toString((short)val);
        }
        else if(type == Integer.TYPE){
            return Integer.toString((int)val);
        }
        else if(type == Long.TYPE){
            return Long.toString((long)val);
        }
        else if(type == Float.TYPE){
            return Float.toString((float)val);
        }
        else if(type == Double.TYPE){
            return Double.toString((double)val);
        }

        return val.toString();
    }

    private String getResourceName(Resource annotation, Class<?> cls){
        String name = annotation.value();

        if(name.isEmpty()){
            name = cls.getSimpleName();
            log.trace("No name specified for resource, using class name [{}] as fallback.", name);
        }

        return name;
    }

    private Object instantiateFromConstructor(Class<?> cls, List<TypeEntry> resourceTypes, Object[] resourceFields, boolean[] flag) throws MagicResourceException {
        Class<?>[] paramTypeArray = new Class<?>[resourceTypes.size()];
        List<Class<?>> requiredTypes = new ArrayList<>();
        List<Object> requiredFields = new ArrayList<>();
        for(int i=0;i<resourceTypes.size();i++){
            TypeEntry entry = resourceTypes.get(i);
            paramTypeArray[i] = entry.type;
            if(entry.isFinal){
                requiredTypes.add(entry.type);
                requiredFields.add(resourceFields[i]);
            }
        }

        //AllArgsConstructor
        try {
            Constructor<?> con = cls.getConstructor(paramTypeArray);
            Object ret = invokeConstructor(con, resourceFields);
            flag[0] = true;
            return ret;
        }
        catch (NoSuchMethodException ignore){}

        Class<?>[] requiredTypesArray = requiredTypes.toArray(new Class<?>[0]);
        Object[] requiredFieldsArray = requiredFields.toArray(new Object[0]);

        //RequiredArgsConstructor
        try {
            Constructor<?> con = cls.getConstructor(requiredTypesArray);
            return invokeConstructor(con, requiredFieldsArray);
        }
        catch (NoSuchMethodException ignore){}

        //EmptyArgsConstructor
        try {
            Constructor<?> con = cls.getConstructor();
            return invokeConstructor(con);
        }
        catch (NoSuchMethodException e){
            throw new MagicResourceException("No suitable constructor found for class: [" + cls.getName() + "].", e);
        }
    }

    private Object invokeConstructor(Constructor<?> con, Object... args) throws MagicResourceException {
        try {
            return con.newInstance(args);
        } catch (InstantiationException e) {
            throw new MagicResourceException("Class is abstract and cannot be instantiated.", e);
        } catch (IllegalAccessException e) {
            throw new MagicResourceException("Constructor is private.", e);
        } catch (InvocationTargetException e) {
            throw new MagicResourceException("Internal exception in constructor!", e);
        }
    }

    private void initInstance(Class<?> cls, Object instance, List<TypeEntry> types, Object[] values) throws MagicResourceException {
        for(int i=0;i<types.size();i++){
            TypeEntry entry = types.get(i);

            if(entry.isFinal) continue;

            Object val = values[i];

            //First try to find a setter
            Method theSetter = null;

            Method[] methods = cls.getMethods();
            for(Method method : methods){
                String name = method.getName();

                if(name.equalsIgnoreCase(entry.name) || name.equalsIgnoreCase("set" + entry.name)){
                    if(method.getParameterCount() == 1 && method.getParameterTypes()[0] == entry.type){
                        theSetter = method;
                        break;
                    }
                }
            }

            if(theSetter != null){
                try {
                    theSetter.invoke(instance, val);
                    continue;
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("INVALID STATE: Cannot be private as it has been checked!");
                } catch (InvocationTargetException e) {
                    throw new MagicResourceException("Internal exception in method [" + theSetter.getName() + "]!", e);
                }
            }

            //Next try to set field directly
            try {
                if(entry.isPrivate) entry.field.setAccessible(true);

                entry.field.set(instance, val);

                if(entry.isPrivate) entry.field.setAccessible(false);
            }
            catch (IllegalAccessException e){
                throw new IllegalStateException("INVALID STATE: Cannot be private as it has been checked!");
            }
        }
    }

    private void saveResource(ResourceEntry entry) throws MagicResourceException {
        List<TypeEntry> resourceTypes = entry.resourceTypes;
        String[] names = new String[resourceTypes.size()];
        Object[] values = new Object[resourceTypes.size()];

        for(int i=0;i<names.length;i++){
            TypeEntry typeEntry = resourceTypes.get(i);
            names[i] = Utils.allToDots(typeEntry.name);
            values[i] = getForEntry(entry.instance, typeEntry);
        }

        entry.result.save(names, values);
    }

    private void saveResource(MergedResourceEntry entry) throws MagicResourceException {
        List<TypeEntry> resourceTypes = entry.resourceTypes;

        class BiSet {
            final String name;
            final Object value;

            public BiSet(String name, Object value) {
                this.name = name;
                this.value = value;
            }
        }

        Map<IResourceLoader.Result, List<BiSet>> map = new HashMap<>();

        for(int i=0;i<resourceTypes.size();i++){
            if(entry.onCloseStates[i] == Resource.ONEXIT_SAVE) {
                TypeEntry typeEntry = resourceTypes.get(i);
                String name = Utils.allToDots(typeEntry.name);
                Object value = getForEntry(entry.instance, typeEntry);

                IResourceLoader.Result res = entry.results[i];
                map.computeIfAbsent(res, r -> new ArrayList<>()).add(new BiSet(name, value));
            }
        }

        for(IResourceLoader.Result res : map.keySet()){
            List<BiSet> sets = map.get(res);
            String[] names = new String[sets.size()];
            Object[] values = new Object[sets.size()];

            for(int i=0;i<sets.size();i++){
                names[i] = sets.get(i).name;
                values[i] = sets.get(i).value;
            }

            res.save(names, values);
        }
    }

    private void saveResource(String path, String[] names, Object[] values) throws MagicResourceException {
        File file = new File(path);

        if(!file.exists()){
            throw new MagicResourceException("Resource file: [" + path + "] does not exist and cannot save!");
        }

        if(path.endsWith(".properties")){
            saveToPropertyFile(file, names, values);
        }
        else if(path.endsWith(".xml")){
            saveToXmlFile(file, names, values);
        }
        else {
            int lastDot = path.lastIndexOf('.');
            String rest = path.substring(lastDot == -1 ? 0 : lastDot);
            throw new MagicResourceException("Invalid file type [" + rest + "]!");
        }
    }

    private void saveToPropertyFile(File file, String[] names, Object[] values) throws MagicResourceException {
        Properties properties = new Properties();

        try(InputStream is = new FileInputStream(file)){
            properties.load(is);
        }
        catch (IOException e){
            throw new MagicResourceException("Could not read file: [" + file.getAbsolutePath() + "]!", e);
        }

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
            for(int i=0;i<names.length;i++){
                String _value = deserializeType(values[i]);
                properties.setProperty(names[i], _value);
            }

            properties.store(bw, null);
        }
        catch (IOException e){
            throw new MagicResourceException("Could not write to file: [" + file.getAbsolutePath() + "]!", e);
        }
    }

    private void saveToXmlFile(File file, String[] names, Object[] values) throws MagicResourceException {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
            bw.append("<xml>\n");
            for(int i=0;i<names.length;i++){
                String name = names[i];
                Object value = values[i];

                printNamedObjectToXmlOut(bw, "   ", name, value);
                bw.append("\n");
            }
            bw.append("</xml>");
        }
        catch (FileNotFoundException e){
            throw new IllegalStateException("INVALID STATE: Should have checked if file exists!", e);
        }
        catch (IOException e){
            throw new MagicResourceException("Exception while writing to file: [" + file.getAbsolutePath() + "]!", e);
        }
    }

    private void printNamedObjectToXmlOut(BufferedWriter bw, String tabs, String name, Object obj) throws IOException {
        bw.append(tabs).append("<").append(name).append(" type=\"").append(obj.getClass().getName()).append("\"").append(">");
        printXmlObject(bw, tabs, obj);
        bw.append("</").append(name).append(">");
    }

    private void printXmlObject(BufferedWriter bw, String oldTabs, Object obj) throws IOException {
        String newTabs = oldTabs + "   ";
        Class<?> type = obj.getClass();

        if(type == Boolean.TYPE || type == Boolean.class
        || type == Character.TYPE || type == Character.class
        || type == Byte.TYPE || type == Byte.class
        || type == Short.TYPE || type == Short.class
        || type == Integer.TYPE || type == Integer.class
        || type == Long.TYPE || type == Long.class
        || type == Float.TYPE || type == Float.class
        || type == Double.TYPE || type == Double.class
        || CharSequence.class.isAssignableFrom(type)){
            String _obj = obj.toString();
            bw.append(_obj);
        }
        else if(obj instanceof List){
            List<?> list = (List<?>) obj;
            bw.append("\n");
            for(int i=0;i<list.size();i++){
                Object lObj = list.get(i);

                printNamedObjectToXmlOut(bw, newTabs, "" + i, lObj);
                bw.append("\n");
            }
            bw.append(oldTabs);
        }
        else {

        }
    }

    private Object getForEntry(Object instance, TypeEntry entry) throws MagicResourceException {
        Class<?> type = entry.type;
        String origName = entry.field.getName();
        Method theGetter = null;

        //First try to find a getter
        Class<?> instanceType = instance.getClass();
        Method[] methods = instanceType.getMethods();
        for(Method method : methods){
            String name = method.getName();

            if(name.equalsIgnoreCase(origName) || name.equalsIgnoreCase("get" + origName)){
                if(method.getParameterCount() == 0 && method.getReturnType() == type){
                    theGetter = method;
                    break;
                }
            }
        }

        if(theGetter != null){
            try {
                return theGetter.invoke(instance);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("INVALID STATE: Cannot be private as it has been checked!");
            } catch (InvocationTargetException e) {
                throw new MagicResourceException("Internal exception in method [" + theGetter.getName() + "]!", e);
            }
        }

        //Next try to get field directly
        try {
            if(entry.isPrivate) entry.field.setAccessible(true);

            Object value = entry.field.get(instance);

            if(entry.isPrivate) entry.field.setAccessible(false);

            return value;
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE: Cannot be private as it has been checked!");
        }
    }

}
