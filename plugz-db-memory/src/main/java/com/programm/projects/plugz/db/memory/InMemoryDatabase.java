package com.programm.projects.plugz.db.memory;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.db.*;

import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

public class InMemoryDatabase implements IDatabaseManager {

    private static boolean isNotSameClass(Class<?> a, Class<?> b){
        if(a == b) return false;

        if(a.isPrimitive()){
            return a != toPrimitive(b);
        }

        if(b.isPrimitive()){
            return b != toPrimitive(a);
        }

        return false;
    }

    private static Class<?> toPrimitive(Class<?> c){
        try {
            return (Class<?>) c.getField("TYPE").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    private static class RepoEntry {
        final Class<?> repoCls;
        final Class<?> dataCls;
        final Class<?> idCls;
        final Map<Method, QueryMethodEntry> queries;

        public RepoEntry(Class<?> repoCls, Class<?> dataCls, Class<?> idCls, Map<Method, QueryMethodEntry> queries) {
            this.repoCls = repoCls;
            this.dataCls = dataCls;
            this.idCls = idCls;
            this.queries = queries;
        }
    }

    private static class QueryMethodEntry {
        final List<List<String>> andOrNames;

        public QueryMethodEntry(List<List<String>> andOrNames) {
            this.andOrNames = andOrNames;
        }
    }

    private static class DataObjectEntry {
        final Class<?> dataCls;
        final Class<?> idCls;
        final DataBuilder builder;
        final List<String> builderTypeNames;
        final Map<String, DataEntry> dataEntryMap;

        public DataObjectEntry(Class<?> dataCls, Class<?> idCls, DataBuilder builder, List<String> builderTypeNames, Map<String, DataEntry> dataEntryMap) {
            this.dataCls = dataCls;
            this.idCls = idCls;
            this.builder = builder;
            this.builderTypeNames = builderTypeNames;
            this.dataEntryMap = dataEntryMap;
        }
    }

    private static class DataEntry {
        final Class<?> type;
        boolean setInBuilder;
        DataGetter getter;
        DataSetter setter;

        public DataEntry(Class<?> type) {
            this.type = type;
        }
    }

    private interface DataBuilder {
        Object build(Object... args) throws InvocationTargetException;
    }

    private static class DataConstructorBuilder implements DataBuilder {
        private final Constructor<?> constructor;

        public DataConstructorBuilder(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public Object build(Object... args) throws InvocationTargetException {
            try {
                return constructor.newInstance(args);
            } catch (InstantiationException e) {
                throw new IllegalStateException("Abstract constructor was annotated with @Builder!");
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("INVALID STATE: should be checked!");
            }
        }
    }

    private static class DataMethodBuilder implements DataBuilder {
        private final Method method;

        public DataMethodBuilder(Method method) {
            this.method = method;
        }

        @Override
        public Object build(Object... args) throws InvocationTargetException {
            try {
                return method.invoke(null, args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("INVALID STATE: should be checked!");
            }
        }
    }

    private interface DataGetter {
        Object get(Object instance) throws InvocationTargetException;
    }

    private static class DataFieldGetter implements DataGetter {
        private final Field field;

        public DataFieldGetter(Field field) {
            this.field = field;
        }

        @Override
        public Object get(Object instance) {
            boolean access = field.canAccess(instance);

            if(!access) field.setAccessible(true);

            Object ret;
            try {
                ret = field.get(instance);
            }
            catch (IllegalAccessException e){
                throw new IllegalStateException("INVALID STATE: cannot access field.");
            }

            if(!access) field.setAccessible(false);

            return ret;
        }
    }

    private static class DataMethodGetter implements DataGetter {
        private final Method method;

        public DataMethodGetter(Method method) {
            this.method = method;
        }

        @Override
        public Object get(Object instance) throws InvocationTargetException{
            boolean access = method.canAccess(instance);

            if(!access) method.setAccessible(true);

            Object ret;
            try {
                ret = method.invoke(instance);
            }
            catch (IllegalAccessException e){
                throw new IllegalStateException("INVALID STATE: cannot access method.");
            }

            if(!access) method.setAccessible(false);

            return ret;
        }
    }

    private interface DataSetter {
        void set(Object instance, Object data) throws InvocationTargetException;
    }

    private static class DataFieldSetter implements DataSetter {
        private final Field field;

        public DataFieldSetter(Field field) {
            this.field = field;
        }

        @Override
        public void set(Object instance, Object data) {
            boolean access = field.canAccess(instance);

            if(!access) field.setAccessible(true);

            try {
                field.set(instance, data);
            }
            catch (IllegalAccessException e){
                throw new IllegalStateException("INVALID STATE: cannot access field.");
            }

            if(!access) field.setAccessible(false);
        }
    }

    private static class DataMethodSetter implements DataSetter {
        private final Method method;

        public DataMethodSetter(Method method) {
            this.method = method;
        }

        @Override
        public void set(Object instance, Object data) throws InvocationTargetException{
            boolean access = method.canAccess(instance);

            if(!access) method.setAccessible(true);

            try {
                method.invoke(instance, data);
            }
            catch (IllegalAccessException e){
                throw new IllegalStateException("INVALID STATE: cannot access method.");
            }

            if(!access) method.setAccessible(false);
        }
    }

    private class RepoHandler implements InvocationHandler {
        private final RepoEntry repoEntry;

        public RepoHandler(RepoEntry repoEntry) {
            this.repoEntry = repoEntry;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String mName = method.getName();
            Class<?> dataCls = repoEntry.dataCls;
            DataObjectEntry objectEntry = entityMap.get(dataCls);

            if(mName.equals("length") || mName.equals("size") || mName.equals("count")){
                Database db = databaseMap.computeIfAbsent(repoEntry.dataCls, c -> new Database());
                return db.data.size();
            }
            else if(mName.equals("create")){
                if(method.getReturnType() != Object.class && method.getReturnType() != dataCls) throw new DataBaseException("Method " + method + " should return [" + dataCls.getName() + "]!");

                if(args.length == 1 && args[0].getClass().isArray()){
                    args = (Object[]) args[0];
                }

                if(args.length != objectEntry.builderTypeNames.size()) throw new DataBaseException("Invalid number of arguments [" + args.length + "] calling method [" + method + "]! Expected [" + objectEntry.builderTypeNames.size() + "].");

                for(int i=0;i<args.length;i++){
                    Object arg = args[i];
                    Class<?> argCls = arg.getClass();
                    String builderArgName = objectEntry.builderTypeNames.get(i);
                    Class<?> builderArgCls = objectEntry.dataEntryMap.get(builderArgName).type;

                    if(isNotSameClass(argCls, builderArgCls)) throw new DataBaseException("Invalid data type! Expected [" + builderArgCls.getName() + "] got [" + argCls.getName() + "] when calling method [" + method + "]!");
                }

                Object o =  objectEntry.builder.build(args);

                Database db = databaseMap.computeIfAbsent(dataCls, c -> new Database());
                db.data.add(o);
                return o;
            }


            QueryMethodEntry entry = repoEntry.queries.get(method);

            if(entry != null){
                Database db = databaseMap.computeIfAbsent(dataCls, c -> new Database());

                if(objectEntry == null) throw new IllegalStateException("SOMETHING WENT WRONG!");

                List<Object> result = getResult(db, entry, objectEntry, args);

                Class<?> retType = method.getReturnType();
                Class<?> declaringClass = method.getDeclaringClass();

                if(declaringClass == ICrudRepo.class){
                    String _retType = retType.getName();
                    Type genericType = method.getGenericReturnType();
                    String _genericType = genericType.getTypeName();

                    if(!_retType.equals(_genericType)){
                        if(_genericType.equals("ID")){
                            retType = repoEntry.idCls;
                        }
                        else if(_genericType.equals("Data")){
                            retType = repoEntry.dataCls;
                        }
                    }
                }

                if(List.class.isAssignableFrom(retType)){
                    return result;
                }
                else if(retType == dataCls){
                    return result.isEmpty() ? null : result.get(0);
                }
                else if(Optional.class.isAssignableFrom(retType)){
                    return Optional.ofNullable(result.isEmpty() ? null : result.get(0));
                }

                throw new DataBaseException("Invalid return type [" + retType + "]! Expected a List<" + dataCls.getSimpleName() + ">, " + dataCls.getSimpleName() + " or an Optional<" + dataCls.getSimpleName() + ">.");
            }

            return method.invoke(proxy, args);
        }

        private List<Object> getResult(Database db, QueryMethodEntry entry, DataObjectEntry objectEntry, Object[] args) throws DataBaseException {
            List<Object> result = new ArrayList<>();

            for(Object o : db.data){
                int argi = 0;
                for(List<String> andNames : entry.andOrNames){
                    boolean is = true;

                    for(String name : andNames){
                        DataEntry dataEntry = objectEntry.dataEntryMap.get(name);

                        if(dataEntry == null) throw new DataBaseException("No such field [" + name + "] found on @Entity [" + repoEntry.dataCls.getName() + "]!");

                        Object queriedObject;
                        try {
                            queriedObject = dataEntry.getter.get(o);
                        }
                        catch (InvocationTargetException e){
                            throw new DataBaseException("Internal Exception getting a value [" + name + "] from @Entity [" + repoEntry.dataCls.getName() + "]!", e);
                        }

                        Object arg = args[argi++];

                        if(!queriedObject.equals(arg)){
                            is = false;
                            break;
                        }
                    }

                    if(is){
                        result.add(o);
                    }

                }
            }

            return  result;
        }
    }


    @Get private ILogger log;

    private final Map<Class<?>, DataObjectEntry> entityMap = new HashMap<>();
    private final Map<Class<?>, Database> databaseMap = new HashMap<>();

    @Override
    public void startup() {
        log.info("In memory database. All data will be lost on exit!");
    }

    @Override
    public void shutdown() {}

    @Override
    public void removeUrl(URL url) {
        //TODO removing urls...
    }

    @Override
    public void registerEntity(Class<?> cls) throws DataBaseException{
        if(!entityMap.containsKey(cls)){
            log.debug("Register Entity: {}", cls);
            try {
                DataObjectEntry dataEntry = createDataEntry(cls);
                entityMap.put(cls, dataEntry);
            }
            catch (DataBaseException e){
                throw new DataBaseException("Exception inspecting the @Entity [" + cls.getName() + "]", e);
            }
        }
    }

    @Override
    public Object registerAndImplementRepo(Class<?> cls) throws DataBaseException {
        if(!IRepo.class.isAssignableFrom(cls)){
            throw new DataBaseException("Must implement the IRepo interface!");
        }

        Type[] genericTypes = cls.getGenericInterfaces();
        ParameterizedType paramTypes = (ParameterizedType)genericTypes[0];

        Type[] actualGenericTypes = paramTypes.getActualTypeArguments();
        Type idType = actualGenericTypes[0];
        Type dataType = actualGenericTypes[1];
        Class<?> idCls = (Class<?>)idType;
        Class<?> dataCls = (Class<?>)dataType;

        RepoEntry repoEntry;
        try {
            DataObjectEntry dataEntry = entityMap.get(dataCls);

            if(dataEntry == null) throw new DataBaseException("Invalid @Entity class [" + dataCls.getName() + "] for @Repo [" + cls.getName() + "]!");

            //Test if same id
            if(isNotSameClass(dataEntry.idCls, idCls)) throw new DataBaseException("The id used in the @Entity [" + dataCls.getName() + "] and the id inside the @Repo [" + cls.getName() + "] do not match! (" + dataEntry.idCls + " <-> " + idCls + ")");

            repoEntry = createRepoEntry(cls, dataEntry);
        }
        catch (DataBaseException e){
            throw new DataBaseException("Exception inspecting the @Entity [" + dataCls.getName() + "]", e);
        }

        RepoHandler handler = new RepoHandler(repoEntry);
        return cls.cast(Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, handler));
    }

    private DataObjectEntry createDataEntry(Class<?> dataCls) throws DataBaseException{
        Map<String, DataEntry> entries = new HashMap<>();
        DataBuilder builder = null;
        List<String> builderTypeNames = new ArrayList<>();

        Map<String, Class<?>> nameTypeMap = new HashMap<>();
        Map<String, Boolean> ignoreMap = new HashMap<>();

        Class<?> idClass = null;
        Constructor<?> builderConstructor = null;
        Method builderMethod = null;

        //Find Builder in constructor
        Constructor<?>[] constructors = dataCls.getConstructors();
        for(Constructor<?> constructor : constructors){
            if(constructor.isAnnotationPresent(Ignore.class)) continue;

            Builder builderAnnotation = constructor.getAnnotation(Builder.class);

            if(builderAnnotation != null) {
                if(builderConstructor != null) throw new DataBaseException("Multiple @Builder Constructors found. Can only have one! (" + constructor + ")");

                String[] paramNames = builderAnnotation.value();
                Class<?>[] params = constructor.getParameterTypes();

                if(paramNames.length != params.length) throw new DataBaseException("Invalid @Builder annotation: Names do not match the length of the parameter count! (" + constructor + ")");

                for(int i=0;i<paramNames.length;i++){
                    String name = paramNames[i];
                    String stdName = nameToStd(name);
                    Class<?> paramCls = params[i];

                    if(nameTypeMap.containsKey(stdName)){
                        throw new DataBaseException("@Builder defines multiple params with the same name: [" + stdName + "]! (" + constructor + ")");
                    }

                    nameTypeMap.put(stdName, paramCls);

                    entries.computeIfAbsent(stdName, n -> new DataEntry(paramCls)).setInBuilder = true;
                    builderTypeNames.add(stdName);
                }

                builderConstructor = constructor;
            }
        }

        Method[] methods = dataCls.getDeclaredMethods();
        for(Method method : methods){
            if(method.isAnnotationPresent(Ignore.class)) continue;

            Builder builderAnnotation = method.getAnnotation(Builder.class);

            if(builderAnnotation != null){
                int mods = method.getModifiers();

                if(!Modifier.isStatic(mods)) throw new DataBaseException("@Builder annotation can only be placed on static methods! (" + method + ")");
                if(builderConstructor != null) throw new DataBaseException("@Builder annotation already found on a constructor! Can only have one! (" + method + ")");
                if(builderMethod != null) throw new DataBaseException("@Builder annotation already found on a method! Can only have one! (" + method + ")");
                if(method.getReturnType() != dataCls) throw new DataBaseException("Static method annotated with @Builder should return the data class: [" + dataCls.getName() + "]! (" + method + ")");

                String[] paramNames = builderAnnotation.value();
                Class<?>[] params = method.getParameterTypes();

                if(paramNames.length != params.length) throw new DataBaseException("Invalid @Builder annotation: Names do not match the length of the parameter count! (" + method + ")");

                for(int i=0;i<paramNames.length;i++){
                    String name = paramNames[i];
                    String stdName = nameToStd(name);
                    Class<?> paramCls = params[i];

                    if(nameTypeMap.containsKey(stdName)){
                        throw new DataBaseException("@Builder defines multiple params with the same name: [" + stdName + "]! (" + method + ")");
                    }

                    nameTypeMap.put(stdName, paramCls);

                    entries.computeIfAbsent(stdName, n -> new DataEntry(paramCls)).setInBuilder = true;
                    builderTypeNames.add(stdName);
                }

                builderMethod = method;
            }
        }

        String builderName;
        if(builderConstructor == null && builderMethod == null){
            try {
                builderConstructor = dataCls.getConstructor();
                builderName = "Constructor: " + builderConstructor;
                builder = new DataConstructorBuilder(builderConstructor);
            } catch (NoSuchMethodException e) {
                throw new DataBaseException("Could neither find a @Builder or an empty constructor for class [" + dataCls.getName() + "]!", e);
            }
        }
        else if(builderConstructor != null){
            builderName = "Constructor: " + builderConstructor;
            builder = new DataConstructorBuilder(builderConstructor);
        }
        else {
            builderName = "Method: " + builderMethod;
            builder = new DataMethodBuilder(builderMethod);
        }

        Field[] fields = dataCls.getDeclaredFields();
        for(Field field : fields){
            String fName = field.getName();
            String stdName = nameToStd(fName);

            if(field.isAnnotationPresent(Ignore.class)) {
                ignoreMap.put(stdName, true);
                continue;
            }

            Class<?> type = field.getType();
            int mods = field.getModifiers();

            Class<?> prevType = nameTypeMap.get(stdName);
            if(prevType != null && type != prevType) throw new DataBaseException("Type for field [" + stdName + "] and previously found type in the builder do not match! (" + type + " <-> " + prevType + ")");

            if(!Modifier.isStatic(mods)) {
                if(Modifier.isFinal(mods)){
                    if(!nameTypeMap.containsKey(stdName)){
                        throw new DataBaseException("Final field [" + stdName + "] is not annotated with @Ignore and is not passed into the @Builder [" + builderName + "]!");
                    }
                }
                else {
                    entries.computeIfAbsent(stdName, n -> new DataEntry(type)).setter = new DataFieldSetter(field);
                }

                if(stdName.equals("id")){
                    if(idClass != null) throw new DataBaseException("Multiple id fields!");
                    idClass = type;
                }

                nameTypeMap.put(stdName, type);
                entries.computeIfAbsent(stdName, n -> new DataEntry(type)).getter = new DataFieldGetter(field);
            }
        }

        for(Method method : methods){
            if(method.isAnnotationPresent(Ignore.class)) continue;

            String mName = method.getName();
            String stdName = nameToStd(mName);

            if(stdName.startsWith("get_") || stdName.startsWith("is_")){
                if(stdName.charAt(2) == '_'){
                    stdName = stdName.substring("is_".length());
                }
                else {
                    stdName = stdName.substring("get_".length());
                }

                if(ignoreMap.containsKey(stdName)) continue;

                Class<?> getType = method.getReturnType();
                Class<?> type = nameTypeMap.get(stdName);

                if(type != null && type != getType) throw new DataBaseException("Getter for field [" + stdName + "] and previously found type do not match! (" + getType + " <-> " + type + ")");

                nameTypeMap.put(stdName, getType);
                entries.computeIfAbsent(stdName, n -> new DataEntry(type)).getter = new DataMethodGetter(method);
            }
            else if(stdName.startsWith("set_")){
                stdName = stdName.substring("set_".length());

                if(ignoreMap.containsKey(stdName)) continue;

                if(method.getParameterCount() != 1) throw new DataBaseException("Invalid setter should accept 1 parameter! (" + method + ")");

                Class<?> setType = method.getParameterTypes()[0];
                Class<?> type = nameTypeMap.get(stdName);

                if(type != null && type != setType) throw new DataBaseException("Setter for field [" + stdName + "] and previously found type do not match! (" + setType + " <-> " + type + ")");

                nameTypeMap.put(stdName, setType);
                entries.computeIfAbsent(stdName, n -> new DataEntry(type)).setter = new DataMethodSetter(method);
            }
        }

        if(idClass == null){
            throw new DataBaseException("Could not find an id for @Data [" + dataCls.getName() + "]!");
        }

        return new DataObjectEntry(dataCls, idClass, builder, builderTypeNames, entries);
    }

    private RepoEntry createRepoEntry(Class<?> cls, DataObjectEntry dataEntry) throws DataBaseException {
        Map<Method, QueryMethodEntry> queryEntries = new HashMap<>();
        collectEntries(cls, dataEntry, queryEntries);

        return new RepoEntry(cls, dataEntry.dataCls, dataEntry.idCls, queryEntries);
    }

    private void collectEntries(Class<?> cls, DataObjectEntry dataEntry, Map<Method, QueryMethodEntry> queryEntries) throws DataBaseException{
        Class<?>[] interfaces = cls.getInterfaces();
        for(Class<?> i : interfaces){
            collectEntries(i, dataEntry, queryEntries);
        }

        Class<?> superCls = cls.getSuperclass();
        if(superCls != null && superCls != Object.class){
            collectEntries(superCls, dataEntry, queryEntries);
        }

        Method[] methods = cls.getDeclaredMethods();
        loopM:
        for(Method method : methods){
            String mName = method.getName();
            String stdName = nameToStd(mName);

            String[] split = stdName.split("_");

            if(mName.equals("length") || mName.equals("size") || mName.equals("count")){
                if(method.getParameterCount() != 0) throw new DataBaseException("Method [" + method + "] to get the number of data stored cannot accept any arguments!");
                Class<?> ret = method.getReturnType();
                if(isNotSameClass(ret, Integer.class) && isNotSameClass(ret, Long.class)) throw new DataBaseException("Method [" + method.getName() + " must return either an integer or a long!]");
                continue;
            }
            else if(mName.equals("create")){
                int paramCount = method.getParameterCount();
                Class<?>[] params = method.getParameterTypes();

                List<String> builderNames = dataEntry.builderTypeNames;

                if(paramCount == 1 && params[0].isArray()){
                    Class<?> arrayType = params[0].getComponentType();

                    if(arrayType == Object.class) continue;


                    for (String builderParamName : builderNames) {
                        Class<?> builderParamType = dataEntry.dataEntryMap.get(builderParamName).type;
                        if (isNotSameClass(arrayType, builderParamType)) throw new DataBaseException("Invalid data type [" + arrayType.getName() + "] for create method in @Repo. Does not match data type [" + builderParamType.getName() + "] of @Builder.");
                    }
                }
                else {
                    if (paramCount != dataEntry.builderTypeNames.size()) throw new DataBaseException("Create method does not match the @Builder for the @Entity [" + dataEntry.dataCls.getName() + "]!");

                    for (int i = 0; i < params.length; i++) {
                        Class<?> paramType = params[i];
                        String builderParamName = dataEntry.builderTypeNames.get(i);
                        Class<?> builderParamType = dataEntry.dataEntryMap.get(builderParamName).type;

                        if (isNotSameClass(paramType, builderParamType)) throw new DataBaseException("Invalid data type [" + paramType.getName() + "] for create method in @Repo. Does not match data type [" + builderParamType.getName() + "] of @Builder.");
                    }
                }

                continue;
            }
            else if(split[0].equals("get") || split[0].equals("find")){
                if(split.length > 1 && split[1].equals("by")){
                    List<List<String>> names = new ArrayList<>();
                    List<String> cur = new ArrayList<>();

                    int pos = 2;
                    do {
                        if (split.length > pos) {
                            cur.add(split[pos]);
                            pos++;
                        }
                        else {
                            break;
                        }

                        if(split.length > pos){
                            if(split[pos].equals("and")){
                                pos++;
                                continue;
                            }
                            else if(split[pos].equals("or")){
                                pos++;
                                names.add(cur);
                                cur = new ArrayList<>();
                                continue;
                            }
                        }
                        else {
                            //Finish
                            names.add(cur);
                            QueryMethodEntry entry = new QueryMethodEntry(new ArrayList<>(names));
                            queryEntries.put(method, entry);
                            names.clear();
                            continue loopM;
                        }

                        break;
                    } while(true);
                }
            }

            throw new DataBaseException("Invalid method: [" + method + "]!");
        }
    }

    /**
     * getById -> get_by_id
     */
    private String nameToStd(String s){
        StringBuilder sb = new StringBuilder();

        for(int i=0;i<s.length();i++){
            char c = s.charAt(i);

            if(Character.isUpperCase(c)){
                sb.append("_").append(Character.toLowerCase(c));
            }
            else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
