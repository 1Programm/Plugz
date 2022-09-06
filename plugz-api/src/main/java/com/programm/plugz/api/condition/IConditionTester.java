package com.programm.plugz.api.condition;

import com.programm.plugz.api.MagicSetupException;

public interface IConditionTester {

    void testCondition(String condition, IConditionContext ctx) throws ConditionFailedException, InvalidConditionException, MagicSetupException;

}
