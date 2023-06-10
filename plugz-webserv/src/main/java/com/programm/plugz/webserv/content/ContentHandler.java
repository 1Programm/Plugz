package com.programm.plugz.webserv.content;

import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.object.mapper.*;

import java.util.*;

public class ContentHandler {

    private final Map<String, IContentReader> readers = new HashMap<>();
    private final Map<String, IContentWriter> writers = new HashMap<>();

    private final Map<Class<?>, Map<Class<?>, IObjectMapper<?, ?>>> specializedMappersMap = new HashMap<>();

    public ContentHandler(){
        ClassAnalyzer analyzer = new ClassAnalyzer(true, false, false);
        ISpecializedObjectMapperLookup mapperLookup = this::getSpecializedMapper;

        readers.put("application/x-www-form-urlencoded", new PlainTextContentReader());
        readers.put("application/json", new JsonContentReader(analyzer, mapperLookup));

        writers.put("application/text", Objects::toString);
        writers.put("text/html", Objects::toString);
        writers.put("text/plain", Objects::toString);
        writers.put("application/json", new JsonContentWriter(analyzer, mapperLookup));
    }

    public IContentReader getReader(String name) {
        return readers.get(name);
    }

    public IContentWriter getWriter(String name) {
        return writers.get(name);
    }

    public boolean supportsMimeType(String type){
        return writers.containsKey(type);
    }

    public void registerSpecializedMapper(Class<?> dataCls, Class<?> entityCls, IObjectMapper<?, ?> specializedMapper) throws ObjectMapException {
        Map<Class<?>, IObjectMapper<?, ?>> specializedMappers = specializedMappersMap.computeIfAbsent(dataCls, c -> new HashMap<>());

        if(specializedMappers.containsKey(entityCls)) throw new ObjectMapException("Multiple mappers for [" + dataCls.getName() + " -> " + entityCls.getName() + "]");
        specializedMappers.put(entityCls, specializedMapper);
    }

    @SuppressWarnings("unchecked")
    private <D, E> IObjectMapper<D, E> getSpecializedMapper(Class<D> dataCls, Class<E> entityCls) {
        Map<Class<?>, IObjectMapper<?, ?>> specializedMappers = specializedMappersMap.get(dataCls);
        if(specializedMappers == null) return null;

        Queue<Class<?>> classes = new ArrayDeque<>();
        classes.add(entityCls);

        while(!classes.isEmpty()) {
            Class<?> cur = classes.poll();

            IObjectMapper<?, ?> mapper = specializedMappers.get(cur);
            if(mapper != null) return (IObjectMapper<D, E>) mapper;

            Class<?> superCls = cur.getSuperclass();
            if(superCls != null){
                classes.add(superCls);
            }

            Class<?>[] interfaces = cur.getInterfaces();
            classes.addAll(Arrays.asList(interfaces));
        }

        return null;
    }
}
