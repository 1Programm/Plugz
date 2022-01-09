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

    static class DataObjectEntry {
        final Class<?> dataCls;
        final Class<?> idCls;
        final Map<String, DataEntry> dataEntryMap;

        public DataObjectEntry(Class<?> dataCls, Class<?> idCls, Map<String, DataEntry> dataEntryMap) {
            this.dataCls = dataCls;
            this.idCls = idCls;
            this.dataEntryMap = dataEntryMap;
        }
    }

    static class DataEntry {
        final Class<?> type;
        DataGetter getter;
        DataSetter setter;

        public DataEntry(Class<?> type) {
            this.type = type;
        }
    }

    interface DataGetter {
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

    interface DataSetter {
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
            IDatabase<Object, Object> db = databaseMap.get(dataCls);

            if(mName.equals("length") || mName.equals("size") || mName.equals("count")){
                return db.size();
            }
            else if(mName.equals("save") || mName.equals("update")){
                Object data = args[0];
                DataEntry fieldEntry = objectEntry.dataEntryMap.get("id");
                Object idValue = fieldEntry.getter.get(data);

                //CREATE NEW AND GENERATE ID
                if(idValue == null){
                    //IF FINAL
                    if(fieldEntry.setter == null){
                        throw new DataBaseException("Id field must be either not null or not final!");
                    }

                    idValue = db.getAdvanceId();
                    fieldEntry.setter.set(data, idValue);
                }

                Class<?> retType = method.getReturnType();

                if(retType == Void.TYPE) {
                    db.save(idValue, data);
                    return null;
                }
                else if(retType.isAssignableFrom(dataCls)) {
                    return db.save(idValue, data);
                }
                else {
                    throw new DataBaseException("Invalid return type [" + retType.getName() + "] for method: [" + method + "]!");
                }
            }
            else if(mName.equals("remove")){
                Object data = args[0];
                db.remove(data);
                return null;
            }


            QueryMethodEntry entry = repoEntry.queries.get(method);

            if(entry != null){
                if(objectEntry == null) throw new IllegalStateException("SOMETHING WENT WRONG!");

                if(entry.andOrNames == null){
                    return db.getAll();
                }

                Map<String, Object> queryArgs = new HashMap<>();
                int arg_i = 0;
                for(List<String> l1 : entry.andOrNames){
                    for(String name : l1){
                        if(!queryArgs.containsKey(name)){
                            if(args == null || arg_i >= args.length) throw new IllegalStateException("SHOULD NOT HAPPEN");
                            Object arg = args[arg_i++];
                            queryArgs.put(name, arg);
                        }
                    }
                }

                List<Object> result = db.query(entry.andOrNames, queryArgs);

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
    }


    @Get private ILogger log;

    private final Map<Class<?>, DataObjectEntry> entityMap = new HashMap<>();
    private final Map<Class<?>, IDatabase<Object, Object>> databaseMap = new HashMap<>();

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

                IDatabase<Object, Object> db = databaseMap.get(cls);
                if(db == null){
                    db = generateDatabase(dataEntry);
                    databaseMap.put(cls, db);
                }
            }
            catch (DataBaseException e){
                throw new DataBaseException("Exception inspecting the @Entity [" + cls.getName() + "]", e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private IDatabase<Object, Object> generateDatabase(DataObjectEntry dataEntry) throws DataBaseException {
        Class<?> idCls = dataEntry.idCls;

        if(idCls == Integer.class){
            return (NumIdDatabase) new NumIdDatabase.IntDatabase<>(dataEntry);
        }
        else if(idCls == Long.class){
            return (NumIdDatabase) new NumIdDatabase.LongDatabase<>(dataEntry);
        }
        else {
            throw new DataBaseException("No database generator found for id-type: [" + idCls.getName() + "]!");
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
            throw new DataBaseException("Exception inspecting the @Repo [" + cls.getName() + "]", e);
        }

        RepoHandler handler = new RepoHandler(repoEntry);
        return cls.cast(Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, handler));
    }

    private DataObjectEntry createDataEntry(Class<?> dataCls) throws DataBaseException{
        Map<String, DataEntry> entries = new HashMap<>();

        Map<String, Class<?>> nameTypeMap = new HashMap<>();
        Map<String, Boolean> ignoreMap = new HashMap<>();

        Class<?> idClass = null;
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

            if(!Modifier.isStatic(mods)) {
                if(!Modifier.isFinal(mods)){
                    entries.computeIfAbsent(stdName, n -> new DataEntry(type)).setter = new DataFieldSetter(field);
                }

                if(fName.equals("id")){
                    if(idClass != null) throw new DataBaseException("Multiple id fields!");
                    if(type.isPrimitive()) throw new DataBaseException("Id cannot be a primitive value as the default value could be interpreted as a set id!");
                    idClass = type;
                }

                nameTypeMap.put(stdName, type);
                entries.computeIfAbsent(stdName, n -> new DataEntry(type)).getter = new DataFieldGetter(field);
            }
        }

        Method[] methods = dataCls.getDeclaredMethods();
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

                if(type != null && isNotSameClass(type, getType)) throw new DataBaseException("Getter for field [" + stdName + "] and previously found type do not match! (" + getType + " <-> " + type + ")");

                nameTypeMap.put(stdName, getType);
                entries.computeIfAbsent(stdName, n -> new DataEntry(type)).getter = new DataMethodGetter(method);
            }
            else if(stdName.startsWith("set_")){
                stdName = stdName.substring("set_".length());

                if(ignoreMap.containsKey(stdName)) continue;

                if(method.getParameterCount() != 1) throw new DataBaseException("Invalid setter should accept 1 parameter! (" + method + ")");

                Class<?> setType = method.getParameterTypes()[0];
                Class<?> type = nameTypeMap.get(stdName);

                if(type != null && isNotSameClass(type, setType)) throw new DataBaseException("Setter for field [" + stdName + "] and previously found type do not match! (" + setType + " <-> " + type + ")");

                nameTypeMap.put(stdName, setType);
                entries.computeIfAbsent(stdName, n -> new DataEntry(type)).setter = new DataMethodSetter(method);
            }
        }

        if(idClass == null){
            throw new DataBaseException("Could not find an id for @Data [" + dataCls.getName() + "]!");
        }

        return new DataObjectEntry(dataCls, idClass, entries);
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

            if(mName.equals("length") || mName.equals("size") || mName.equals("count")){
                if(method.getParameterCount() != 0) throw new DataBaseException("Method [" + method + "] to get the number of data stored cannot accept any arguments!");
                Class<?> ret = method.getReturnType();
                if(isNotSameClass(ret, Integer.class) && isNotSameClass(ret, Long.class)) throw new DataBaseException("Method [" + method.getName() + " must return either an integer or a long!]");
                continue;
            }
            else if(mName.equals("save") || mName.equals("update")){
                if(method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(dataEntry.dataCls)) throw new DataBaseException("The '" + mName + "' Method [" + method + "] must accept exactly 1 argument: [" + dataEntry.dataCls + "]!");
                if(method.getReturnType() != Void.TYPE && !method.getReturnType().isAssignableFrom(dataEntry.dataCls)) throw new DataBaseException("The '" + mName + "' Method [" + method + "] must return either void or type of [" + dataEntry.dataCls + "]!");
                continue;
            }
            else if(mName.equals("remove") || mName.equals("delete")){
                if(method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(dataEntry.dataCls)) throw new DataBaseException("The '" + mName + "' Method [" + method + "] must accept exactly 1 argument: [" + dataEntry.dataCls + "]!");
                continue;
            }

            String[] split = stdName.split("_");

            if(split[0].equals("get") || split[0].equals("find")){
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
                else if(split.length == 2 && split[1].equals("all")){
                    if(method.getParameterCount() != 0) throw new DataBaseException("Query-all methods should not get any arguments!");
                    if(!List.class.isAssignableFrom(method.getReturnType())) throw new DataBaseException("Query-all methods must return some kind of List!");

                    queryEntries.put(method, new QueryMethodEntry(null));
                    continue;
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
