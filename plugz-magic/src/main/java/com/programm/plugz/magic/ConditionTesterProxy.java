package com.programm.plugz.magic;

import com.programm.plugz.api.MagicSetupException;
import com.programm.plugz.api.condition.ConditionFailedException;
import com.programm.plugz.api.condition.IConditionContext;
import com.programm.plugz.api.condition.IConditionTester;
import com.programm.plugz.api.condition.InvalidConditionException;

class ConditionTesterProxy implements IConditionTester {

    public IConditionTester tester;

    @Override
    public void testCondition(String condition, IConditionContext ctx) throws ConditionFailedException, InvalidConditionException, MagicSetupException {
        if(tester == null) throw new MagicSetupException("No condition tester registered!");
        tester.testCondition(condition, ctx);
    }
}
