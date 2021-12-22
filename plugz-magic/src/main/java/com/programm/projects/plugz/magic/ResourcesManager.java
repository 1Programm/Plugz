package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.resource.DefaultResourceMerger;
import com.programm.projects.plugz.magic.resource.MagicResourceException;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

@RequiredArgsConstructor
@Logger("Resources-Manager")
class ResourcesManager {

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

    @RequiredArgsConstructor
    private static abstract class AbstractResult implements IResourceLoader.Result {
        final Object[] values;

        @Override
        public int size() {
            return values.length;
        }

        @Override
        public Object get(int i) {
            return values[i];
        }
    }

    @RequiredArgsConstructor
    private static class ResourceEntry {
        final Object instance;
        final String name;
        final List<TypeEntry> resourceTypes;
        final IResourceLoader.Result result;
    }

    @RequiredArgsConstructor
    private static class MergedResourceEntry {
        final Object instance;
        final String[] names;
        final List<TypeEntry> resourceTypes;
        final IResourceLoader.Result[] results;
        final int[] onCloseStates;
    }

    @RequiredArgsConstructor
    private static class TypeEntry implements IResourceLoader.Entry {
        final Field field;
        final Class<?> type;
        final String name;
        final boolean isFinal;
        final boolean isPrivate;
        final IValueFallback fallback;

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

    private final ILogger log;
    private final MagicInstanceManager instanceManager;

    private final Map<URL, List<ResourceEntry>> saveOnExitMap = new HashMap<>();
    private final Map<URL, List<MergedResourceEntry>> saveOnExitMergedMap = new HashMap<>();

    public void shutdown() throws MagicResourceException {
        for(URL url : saveOnExitMap.keySet()) {
            List<ResourceEntry> entries = saveOnExitMap.get(url);

            log.debug("Saving [{}] resources from url [{}]...", entries.size(), url);

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

        for(URL url : saveOnExitMergedMap.keySet()) {
            List<MergedResourceEntry> entries = saveOnExitMergedMap.get(url);

            log.debug("Saving [{}] resources from url [{}]...", entries.size(), url);

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
    }

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

    public Object buildResourceObject(Class<?> cls) throws MagicResourceException {
        Resource resourceAnnotation = cls.getAnnotation(Resource.class);

        if(resourceAnnotation == null) throw new IllegalStateException("INVALID STATE: Class: [" + cls.getName() + "] should be annotated with @Resource but was not.");

        String resName = getResourceName(resourceAnnotation, cls);
//        boolean resIsRunResource = resourceAnnotation.path().isEmpty();
        int resOnClose = resourceAnnotation.onexit();
        int resNotFound = resourceAnnotation.notfound();
        Class<? extends IResourceLoader> resLoader = resourceAnnotation.loader();

        URL url = Utils.getUrlFromClass(cls);

        List<TypeEntry> resourceTypes = collectTypeEntries(cls);
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
//            if(resIsRunResource){
//                log.error("Cannot register resource to save it because it is a static runtime resource!");
//            }
//            else {
                log.debug("Registering resource class: [{}] to save on shutdown.", cls.getName());
                saveOnExitMap.computeIfAbsent(url, u -> new ArrayList<>()).add(instanceEntry);
//            }
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    private IResourceLoader.Result getResourceFields(String resName, int resNotFound, List<TypeEntry> resourceTypes, Class<? extends IResourceLoader> resLoader, Object[] sourceBack) throws MagicResourceException{
        if(resLoader == IResourceLoader.class) {
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
            is = ResourcesManager.class.getResourceAsStream(name);
        }
        else {
            is = ResourcesManager.class.getResourceAsStream("/" + name);
        }

        if(is != null){
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
        if(name.endsWith(".properties")){
            Properties properties = new Properties();
            try {
                properties.load(is);
            }
            catch (IOException e){
                throw new MagicResourceException("Could not load resource [" + name + "] from properties file.", e);
            }

            sourceBack[0] = properties;

            return loadResourceFieldsFromProperties(properties, types);
        }

        return null;
    }

    private Object[] loadResourceFieldsFromProperties(Properties properties, List<TypeEntry> types) throws MagicResourceException {
        Object[] values = new Object[types.size()];

        for(int i=0;i<types.size();i++){
            TypeEntry entry = types.get(i);
            String name = entry.name;
            String _val = properties.getProperty(name);

            if(_val == null){
                for(NameFallback nameFallback : NAME_FALLBACK_TRIES){
                    String _name = nameFallback.parse(name);
                    _val = properties.getProperty(_name);

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

    private Object serializeType(Class<?> type, String _val) throws MagicResourceException {
        if(type == Boolean.TYPE){
            return Boolean.parseBoolean(_val);
        }
        else if(type == Character.TYPE){
            if(_val.length() != 1){
                throw new MagicResourceException("Parsing exception: Expected a character, got: [" + _val + "].");
            }

            return _val.charAt(0);
        }
        else if(type == Byte.TYPE){
            try {
                return Byte.parseByte(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a byte, got: [" + _val + "].");
            }
        }
        else if(type == Short.TYPE){
            try {
                return Short.parseShort(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a short, got: [" + _val + "].");
            }
        }
        else if(type == Integer.TYPE){
            try {
                return Integer.parseInt(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected an int, got: [" + _val + "].");
            }
        }
        else if(type == Long.TYPE){
            try {
                return Long.parseLong(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a long, got: [" + _val + "].");
            }
        }
        else if(type == Float.TYPE){
            try {
                return Float.parseFloat(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a float, got: [" + _val + "].");
            }
        }
        else if(type == Double.TYPE){
            try {
                return Double.parseDouble(_val);
            }
            catch (NumberFormatException e){
                throw new MagicResourceException("Parsing exception: Expected a double, got: [" + _val + "].");
            }
        }

        return _val;
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

//    private void saveResource(ResourceEntry entry) throws MagicResourceException {
//        String name = entry.name;
//        File file = new File(name);
//
//        if(!file.exists()){
//            throw new MagicResourceException("Resource file: [" + name + "] does not exist and cannot save!");
//        }
//
//        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
//            if(name.endsWith(".properties")){
//                saveToPropertyFile(bw, entry);
//            }
//        }
//        catch (IOException e){
//            throw new MagicResourceException("Could not write to file: [" + name + "]!", e);
//        }
//    }

//    private void saveToPropertyFile(BufferedWriter bw, ResourceEntry entry) throws IOException, MagicResourceException {
//        Properties properties = new Properties();
//        Object instance = entry.instance;
//        List<TypeEntry> resourceTypes = entry.resourceTypes;
//
//        for(TypeEntry typeEntry : resourceTypes){
//            Object value = getForEntry(instance, typeEntry);
//            String _value = "";//deserializeType(typeEntry.type, value);
//
//            String key = Utils.allToDots(typeEntry.name);
//
//            properties.setProperty(key, _value);
//        }
//
//        properties.store(bw, null);
//    }

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

        @RequiredArgsConstructor
        class BiSet {
            final String name;
            final Object value;
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

    private Object getForEntry(Object instance, TypeEntry entry) throws MagicResourceException {
        Class<?> type = entry.type;
        String origName = entry.field.getName();
        Method theGetter = null;

        //First try to find a getter
        Method[] methods = type.getMethods();
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
