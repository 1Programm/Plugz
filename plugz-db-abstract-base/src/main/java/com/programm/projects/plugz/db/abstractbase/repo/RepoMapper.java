package com.programm.projects.plugz.db.abstractbase.repo;

import com.programm.projects.plugz.db.abstractbase.entity.EntityEntry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.programm.projects.plugz.db.abstractbase.DBBaseUtils.isNotSameClass;
import static com.programm.projects.plugz.db.abstractbase.DBBaseUtils.nameToStd;

public class RepoMapper {

    public static RepoEntry createEntry(Class<?> cls, Class<?> idCls, EntityEntry entityEntry) throws RepoMapException {
        //Test if same id
        if(isNotSameClass(entityEntry.getIdCls(), idCls)) throw new RepoMapException("The id used in the @Entity [" + entityEntry.getEntityCls().getName() + "] and the id inside the @Repo [" + cls.getName() + "] do not match! (" + entityEntry.getIdCls() + " <-> " + idCls + ")");

        Map<Method, String> queryEntries = new HashMap<>();
        collectEntries(cls, entityEntry, queryEntries);

        return new RepoEntry(cls, entityEntry.getEntityCls(), idCls, queryEntries);
    }

    private static void collectEntries(Class<?> cls, EntityEntry entityEntry, Map<Method, String> queryMap) throws RepoMapException {
        Class<?>[] interfaces = cls.getInterfaces();
        for(Class<?> i : interfaces){
            collectEntries(i, entityEntry, queryMap);
        }

        Class<?> superCls = cls.getSuperclass();
        if(superCls != null && superCls != Object.class){
            collectEntries(superCls, entityEntry, queryMap);
        }

        Class<?> entityCls = entityEntry.getEntityCls();

        Method[] methods = cls.getDeclaredMethods();
        loopM:
        for(Method method : methods){
            String mName = method.getName();
            String stdName = nameToStd(mName);
            int paramCount = method.getParameterCount();
            Class<?> ret = method.getReturnType();

            switch (mName) {
                case "length":
                case "size":
                case "count":
                    if (paramCount != 0) throw new RepoMapException("Method [" + method + "] to get the number of data stored cannot accept any arguments!");
                    if (isNotSameClass(ret, Integer.class) && isNotSameClass(ret, Long.class)) throw new RepoMapException("Method [" + method.getName() + " must return either an integer or a long!]");
                    queryMap.put(method, "count all");
                    continue;
                case "save":
                case "update":
                    if (paramCount != 1 || !method.getParameterTypes()[0].isAssignableFrom(entityCls)) throw new RepoMapException("The '" + mName + "' Method [" + method + "] must accept exactly 1 argument: [" + entityCls + "]!");
                    if (ret != Void.TYPE && !ret.isAssignableFrom(entityCls)) throw new RepoMapException("The '" + mName + "' Method [" + method + "] must return either void or type of [" + entityCls + "]!");
                    queryMap.put(method, "update");
                    continue;
                case "remove":
                case "delete":
                    if (paramCount != 1 || !method.getParameterTypes()[0].isAssignableFrom(entityCls)) throw new RepoMapException("The '" + mName + "' Method [" + method + "] must accept exactly 1 argument: [" + entityCls + "]!");
                    queryMap.put(method, "delete");
                    continue;
            }

            String[] split = stdName.split("_");

            if(split[0].equals("get") || split[0].equals("find")){
                if(split.length > 1 && split[1].equals("by")){
                    List<List<String>> names = new ArrayList<>();
                    List<String> cur = new ArrayList<>();

                    int pos = 2;
                    while(true) {
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
                            StringBuilder sb1 = new StringBuilder();
                            StringBuilder sb2 = new StringBuilder();

                            for(List<String> ands : names){
                                if(sb1.length() != 0) sb1.append(" | ");
                                boolean l1 = ands.size() == 1;

                                if(!l1) sb1.append("(");

                                sb2.setLength(0);
                                for(String name : ands){
                                    if(sb2.length() != 0) sb2.append(" & ");
                                    sb2.append(name);
                                }
                                sb1.append(sb2);

                                if(!l1) sb1.append(")");
                            }

                            String query = "find by " + sb1;
                            queryMap.put(method, query);
                            names.clear();
                            continue loopM;
                        }

                        break;
                    }
                }
                else if(split.length == 2 && split[1].equals("all")){
                    if(method.getParameterCount() != 0) throw new RepoMapException("Query-all methods should not get any arguments!");
                    if(!List.class.isAssignableFrom(method.getReturnType())) throw new RepoMapException("Query-all methods must return some kind of List!");
                    queryMap.put(method, "find all");
                    continue;
                }
            }

            throw new RepoMapException("Invalid method: [" + method + "]!");
        }
    }

}
