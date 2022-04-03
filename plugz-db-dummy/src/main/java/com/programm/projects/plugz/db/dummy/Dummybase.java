package com.programm.projects.plugz.db.dummy;

import com.programm.projects.plugz.db.abstractbase.entity.EntityEntry;
import com.programm.projects.plugz.db.abstractbase.entity.FieldEntry;
import com.programm.projects.plugz.db.abstractbase.repo.IQueryExecutor;
import com.programm.projects.plugz.db.abstractbase.repo.QueryExecuteException;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@RequiredArgsConstructor
public class Dummybase implements IQueryExecutor {

    private final EntityEntry entityEntry;

    private final List<Object> data = new ArrayList<>();
    private final Map<String, Map<Object, List<Integer>>> dataMaps = new HashMap<>();

    public void setup(){
        for(String name : entityEntry.getFieldEntryMap().keySet()){
            dataMaps.put(name, new HashMap<>());
        }
    }

    @Override
    public Object execute(String query, Object... args) throws QueryExecuteException {
        if(query.equals("count all")){
            return data.size();
        }
        else if(query.startsWith("find by")){
            String andOrQuery = query.substring("find by".length());

            Set<Integer> retDataIndices = new HashSet<>();
            Map<String, Integer> tmp = new HashMap<>();

            String[] orSplit = andOrQuery.split("\\|");
            for(String orQuery : orSplit){
                String[] andSplit = orQuery.split("&");

                Set<Integer> tmpId1 = new HashSet<>();
                Set<Integer> tmpId2 = new HashSet<>();

                for(String term : andSplit){
                    term = term.trim();
                    Integer i = tmp.get(term);
                    if(i == null){
                        i = tmp.size();
                        tmp.put(term, i);
                    }

                    if(i >= args.length) throw new QueryExecuteException("Not enough arguments for query: [" + query + "]!");
                    Object arg = args[i];

                    Map<Object, List<Integer>> valueToIdsMap = dataMaps.get(term);

                    if(valueToIdsMap == null) throw new QueryExecuteException("No such term found: [" + term + "] for entity: [" + entityEntry.getEntityCls() + "]!");

                    List<Integer> ids = valueToIdsMap.get(arg);

                    if(ids != null) {
                        if(tmpId2.isEmpty()){
                            tmpId1.addAll(ids);
                        }
                        else {
                            for(Integer num : ids){
                                if(tmpId2.contains(num)){
                                    tmpId1.add(num);
                                }
                            }
                        }

                        tmpId2 = tmpId1;
                        tmpId1 = new HashSet<>();
                    }
                }

                if(!tmpId2.isEmpty()){
                    retDataIndices.addAll(tmpId2);
                }
            }


            if(retDataIndices.isEmpty()) return null;
            List<Object> retData = new ArrayList<>();

            for(int index : retDataIndices){
                retData.add(data.get(index));
            }

            if(retData.size() == 1) return retData.get(0);

            return retData;
        }
        else if(query.equals("update")){
            Object data = args[0];
            int index = this.data.indexOf(data);

            if(index == -1){
                index = this.data.size();
                this.data.add(data);
            }


            for(Map.Entry<String, FieldEntry> fieldEntry : entityEntry.getFieldEntryMap().entrySet()){
                try {
                    Object fieldData = fieldEntry.getValue().getGetter().get(data);
                    dataMaps.get(fieldEntry.getKey()).computeIfAbsent(fieldData, d -> new ArrayList<>()).add(index);
                }
                catch (InvocationTargetException e){
                    throw new QueryExecuteException("Failed to retrieve data from field [" + fieldEntry.getKey() + "] from object: [" + data + "].", e);
                }
            }

            return data;
        }

        throw new QueryExecuteException("Invalid query: [" + query + "]!");
    }
}
