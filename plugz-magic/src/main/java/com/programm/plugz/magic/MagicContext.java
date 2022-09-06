package com.programm.plugz.magic;

import com.programm.plugz.api.ISubsystem;
import com.programm.plugz.api.MagicSetupException;
import com.programm.plugz.api.PlugzConfig;
import com.programm.plugz.api.condition.ConditionFailedException;
import com.programm.plugz.api.condition.IConditionContext;
import com.programm.plugz.api.condition.IConditionTester;
import com.programm.plugz.api.condition.InvalidConditionException;
import lombok.RequiredArgsConstructor;

import java.net.URL;
import java.util.List;

@RequiredArgsConstructor
class MagicContext implements IConditionContext {

    public final PlugzConfig config;
    public IConditionTester conditionTester;
    public MagicInstanceManager instanceManager;
    public List<URL> scanUrls;
    public List<Class<?>> subsystems;


    public void testCondition(String condition) throws ConditionFailedException, InvalidConditionException, MagicSetupException {
        conditionTester.testCondition(condition, this);
    }

    //======== CONDITION CONTEXT ========//

    @Override
    public String getConfig(String name) {
        return config.get(name);
    }

    @Override
    public boolean hasInstanceOfType(Class<?> type) {
        return instanceManager.hasInstanceOfType(type);
    }

    @Override
    public List<URL> scanUrls() {
        return scanUrls;
    }

    @Override
    public List<Class<?>> subsystems() {
        return subsystems;
    }

    //======== CONDITION CONTEXT ========//
}
