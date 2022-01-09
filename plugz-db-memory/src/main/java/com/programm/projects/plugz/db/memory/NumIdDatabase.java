package com.programm.projects.plugz.db.memory;

import com.programm.projects.plugz.magic.api.db.DataBaseException;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

abstract class NumIdDatabase<ID, Data> implements IDatabase<ID, Data> {

    public static final class IntDatabase <Data> extends NumIdDatabase<Integer, Data> {
        private Integer currentId = 0;

        public IntDatabase(InMemoryDatabase.DataObjectEntry dataEntry) {
            super(dataEntry);
        }

        @Override
        public Integer getAdvanceId() {
            return currentId++;
        }
    }

    public static final class LongDatabase <Data> extends NumIdDatabase<Long, Data> {
        private Long currentId = 0L;

        public LongDatabase(InMemoryDatabase.DataObjectEntry dataEntry) {
            super(dataEntry);
        }

        @Override
        public Long getAdvanceId() {
            return currentId++;
        }
    }

    private final List<Data> dataList = new ArrayList<>();
    private final Map<String, Map<Object, List<Integer>>> posMap = new HashMap<>();
    private final InMemoryDatabase.DataObjectEntry dataEntry;

    private final Map<Data, Map<String, Object>> cachedData = new HashMap<>();

    private int size;

    public NumIdDatabase(InMemoryDatabase.DataObjectEntry dataEntry) {
        this.dataEntry = dataEntry;

        for(String fieldName : dataEntry.dataEntryMap.keySet()){
            posMap.put(fieldName, new HashMap<>());
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Data save(ID id, Data data) throws DataBaseException {
        int pos;

        Map<String, Object> cache = cachedData.get(data);
        Map<Object, List<Integer>> idMap = posMap.get("id");

        List<Integer> prevPosList = idMap.get(id);
        if(prevPosList == null){
            pos = dataList.size();
            dataList.add(data);
            size++;
            idMap.put(id, Collections.singletonList(pos));
        }
        else {
            int prevPos = prevPosList.get(0); // Should only be 1 element
            Data prevData = dataList.get(prevPos);

            if(data != prevData){
                cache = cachedData.get(prevData);
            }

            for(String name : posMap.keySet()) {
                if(name.equals("id")) continue;
                Object val = cache.get(name);
                Map<Object, List<Integer>> valMap = posMap.get(name);

                List<Integer> positions = valMap.get(val);
                if(positions != null){
                    positions.remove((Object)prevPos);
                    if(positions.isEmpty()){
                        valMap.remove(val);
                    }
                }
            }

            pos = prevPos;

            if(prevData != data){
                dataList.set(prevPos, data);
                cachedData.remove(prevData);
                cachedData.put(data, cache);
            }
        }

        if(cache == null){
            cache = new HashMap<>();
            cachedData.put(data, cache);
            cache.put("id", id);
        }

        for(String name : posMap.keySet()){
            if(name.equals("id")) continue;

            Map<Object, List<Integer>> valMap = posMap.get(name);

            InMemoryDatabase.DataEntry fieldEntry = dataEntry.dataEntryMap.get(name);
            try {
                Object val = fieldEntry.getter.get(data);
                cache.put(name, val);

                valMap.computeIfAbsent(val, v -> new ArrayList<>()).add(pos);
            }
            catch (InvocationTargetException e){
                throw new DataBaseException("", e);
            }
        }

        return data;
    }

    @Override
    public void remove(Data data) throws DataBaseException {
        Map<String, Object> cache = cachedData.remove(data);

        int pos;
        if(cache == null){
            Object id;

            try {
                id = dataEntry.dataEntryMap.get("id").getter.get(data);
            }
            catch (InvocationTargetException e){
                throw new DataBaseException("", e);
            }

            pos = posMap.get("id").remove(id).get(0);
            data = dataList.set(pos, null);
            cache = cachedData.remove(data);
        }
        else {
            Object id = cache.get("id");
            pos = posMap.get("id").remove(id).get(0);
            dataList.set(pos, null);
        }

        for(String name : posMap.keySet()) {
            if(name.equals("id")) continue;
            Map<Object, List<Integer>> valMap = posMap.get(name);

            Object val = cache.get(name);
            List<Integer> positions = valMap.get(val);

            if(positions != null){
                positions.remove((Object)pos);
                if(positions.isEmpty()){
                    valMap.remove(val);
                }
            }
        }

        size--;
    }

    @Override
    public List<Data> getAll() {
        return dataList;
    }

    @Override
    public List<Data> query(List<List<String>> query, Map<String, Object> queryArgs) {
        Set<Integer> positions = new HashSet<>();

        for(List<String> andQuery : query){
            queryAnd(andQuery, positions, queryArgs);
        }

        List<Data> result = new ArrayList<>();

        for(int pos : positions){
            result.add(dataList.get(pos));
        }

        return result;
    }

    private void queryAnd(List<String> query, Set<Integer> result, Map<String, Object> queryArgs){
        Map<Integer, Integer> map = new HashMap<>();

        for(String name : query){
            Map<Object, List<Integer>> valueMap = posMap.get(name);

            Object arg = queryArgs.get(name);
            List<Integer> res = valueMap.get(arg);

            if(res != null) {
                for (int index : res) {
                    int count = map.computeIfAbsent(index, i -> 0);
                    map.put(index, count + 1);
                }
            }
        }

        for(int index : map.keySet()){
            int count = map.get(index);
            if(count == query.size()){
                result.add(index);
            }
        }
    }
}
