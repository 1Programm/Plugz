package com.programm.plugz.magic;

import com.programm.plugz.api.MagicSetupException;
import com.programm.plugz.api.condition.ConditionFailedException;
import com.programm.plugz.api.condition.IConditionContext;
import com.programm.plugz.api.condition.IConditionTester;
import com.programm.plugz.api.condition.InvalidConditionException;

public class SimpleConditionTester implements IConditionTester {

    /*
    Config values
    ${a}
    ${abc.d.ef}
    !${a}
    ${a} == 10
    ${a} != true
    ${a} ?= a[bc]d

    Subsystem present check
    [subsystemName]
    [name]
    ![bla]
     */

    @Override
    public void testCondition(String condition, IConditionContext ctx) throws ConditionFailedException, InvalidConditionException, MagicSetupException {
        if(condition.startsWith("${")){
            int nextClosing = condition.indexOf('}', 2);
            if(nextClosing == -1) throw new InvalidConditionException("Invalid condition ending: \"" + condition + "\". Expected '}'.");

            String configName = condition.substring(2, nextClosing);
            String configValue = ctx.getConfig(configName);

            if(nextClosing < condition.length() - 1){
                int comparatorStart = advanceWhitespace(condition, nextClosing + 1);
                int startRest = advanceWhitespace(condition, comparatorStart + 2);
                if(startRest == condition.length()) throw new InvalidConditionException("Invalid condition ending: \"" + condition + "\". Expected some value to compare!");

                String rest = condition.substring(startRest);

                if(condition.startsWith("==", comparatorStart)){
                    if(configValue == null) throw new ConditionFailedException(condition, "No such config defined: [" + configName + "]");
                    if(!configValue.equals(rest)){
                        throw new ConditionFailedException(condition, "[" + configValue + "] does not equal [" + rest + "].");
                    }
                }
                else if(condition.startsWith("!=", comparatorStart)){
                    if(configValue == null) throw new ConditionFailedException(condition, "No such config defined: [" + configName + "]");
                    if(configValue.equals(rest)){
                        throw new ConditionFailedException(condition, "[" + configValue + "] equals [" + rest + "].");
                    }
                }
                else if(condition.startsWith("?=", comparatorStart)){
                    if(configValue == null) throw new ConditionFailedException(condition, "No such config defined: [" + configName + "]");
                    if(!configValue.matches(rest)){
                        throw new ConditionFailedException(condition, "[" + configValue + "] does not match the regex [" + rest + "].");
                    }
                }
                else {
                    throw new InvalidConditionException("Invalid comparator in condition: \"" + condition + "\".");
                }
            }
            else {
                if(configValue == null) throw new ConditionFailedException(condition, "Config [" + configName + "] not present!");
            }
        }
        else if(condition.startsWith("!${")){
            if(!condition.endsWith("}")) throw new InvalidConditionException("Invalid condition ending: \"" + condition + "\". Expected a '}' at the end.");
            String configName = condition.substring(2, condition.length() - 1);
            String configValue = ctx.getConfig(configName);
            if(configValue != null) throw new ConditionFailedException(condition, "Config [" + configName + "] is present!");
        }
        else if(condition.startsWith("[")){
            if(!condition.endsWith("]")) throw new InvalidConditionException("Invalid condition ending: \"" + condition + "\". Expected a ']' at the end.");
            String subsystemName = condition.substring(1, condition.length() - 1);
            if(!isSubsystemPresent(ctx, subsystemName)){
                throw new ConditionFailedException(condition, "Subsystem [" + subsystemName + "] is not present!");
            }
        }
        else if(condition.startsWith("![")){
            if(!condition.endsWith("]")) throw new InvalidConditionException("Invalid condition ending: \"" + condition + "\". Expected a ']' at the end.");
            String subsystemName = condition.substring(1, condition.length() - 1);
            if(isSubsystemPresent(ctx, subsystemName)){
                throw new ConditionFailedException(condition, "Subsystem [" + subsystemName + "] is present!");
            }
        }
        else {
            throw new InvalidConditionException("Invalid condition start: \"" + condition + "\"!");
        }
    }

    private int advanceWhitespace(String s, int index){
        if(index >= s.length()) return index;

        while(Character.isWhitespace(s.charAt(index))) index++;
        return index;
    }

    private boolean isSubsystemPresent(IConditionContext ctx, String subsystemName){
        boolean hasSubsystemPartInName = false;

        if(subsystemName.contains("Subsystem")){
            hasSubsystemPartInName = true;
        }

        for(Class<?> subsystemClass : ctx.subsystems()){
            if(hasSubsystemPartInName){
                if(subsystemClass.getSimpleName().equals(subsystemName)) return true;
            }
            else {
                String clsName = removeSubsystemWordInName(subsystemClass.getSimpleName());
                if(clsName.equals(subsystemName)) return true;
            }
        }

        return false;
    }

    private String removeSubsystemWordInName(String name){
        int pos = name.indexOf("_subsystem");
        if(pos != -1) return name.substring(0, pos) + name.substring(pos + "_subsystem".length());

        pos = name.indexOf("subsystem");
        if(pos != -1) return name.substring(0, pos) + name.substring(pos + "subsystem".length());

        pos = name.indexOf("_Subsystem");
        if(pos != -1) return name.substring(0, pos) + name.substring(pos + "_Subsystem".length());

        pos = name.indexOf("Subsystem");
        if(pos != -1) return name.substring(0, pos) + name.substring(pos + "Subsystem".length());

        return name;
    }

}
