package com.programm.plugz.events;

import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.plugz.api.instance.MagicMethod;
import com.programm.plugz.api.utils.ValueParseException;
import com.programm.plugz.api.utils.ValueUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

class EventListenerMethodImpl implements IEventListener {

    private final MagicMethod magicMethod;
    private final Method method;

    public EventListenerMethodImpl(MagicMethod magicMethod, Method method) {
        this.magicMethod = magicMethod;
        this.method = method;
    }

    @Override
    public void onEvent(Object... args) {
        if(args == null || args.length == 0){
            try {
                magicMethod.invoke();
            }
            catch (MagicInstanceException e){
                throw new MagicRuntimeException("Failed to run event listener [" + magicMethod + "]!", e);
            }
        }
        else {
            int mArgsC = magicMethod.nonMagicArgsCount();

            Object[] eventArgs = new Object[mArgsC];
            Parameter[] parameters = method.getParameters();

            int eventArgsC = 0;
            for(Parameter parameter : parameters){
                if(!parameter.isAnnotationPresent(Get.class) && !parameter.isAnnotationPresent(GetConfig.class)){
                    Class<?> paramType = parameter.getType();
                    Object value;
                    if(eventArgsC < args.length){
                        value = args[eventArgsC];
                        try {
                            value = tryPassPrimitive(value, paramType);
                        }
                        catch (ValueParseException e){
                            throw new MagicRuntimeException("Failed to parse event arguments to fit the listener parameters! (" + args[eventArgsC].getClass().getName() + " -> " + paramType.getName() + ")", e);
                        }
                    }
                    else {
                        value = ValueUtils.getDefaultValue(paramType);
                    }

                    eventArgs[eventArgsC++] = value;
                }
            }

            try {
                magicMethod.invoke(eventArgs);
            }
            catch (MagicInstanceException e){
                throw new MagicRuntimeException("Failed to run event listener [" + magicMethod + "]!", e);
            }
        }
    }

    private Object tryPassPrimitive(Object value, Class<?> targetCls) {
        if(value == null) return null;
        Class<?> valueCls = value.getClass();

        if(targetCls.isAssignableFrom(valueCls) || isSame(valueCls, targetCls)) return value;

        boolean isValuePrimitive = valueCls.isPrimitive();
        boolean isTargetPrimitive = targetCls.isPrimitive();

        if(!isValuePrimitive && valueCls == String.class){
            isValuePrimitive = true;
        }

        if(!isTargetPrimitive && targetCls == String.class){
            isTargetPrimitive = true;
        }

        if(isValuePrimitive && isTargetPrimitive){
            return ValueUtils.parsePrimitive(value, targetCls);
        }

        return null;
    }

    private boolean isSame(Class<?> a, Class<?> b){
        if(a == b) return true;
        a = ValueUtils.unwrapPrimitiveWrapper(a);
        b = ValueUtils.unwrapPrimitiveWrapper(b);
        return a == b;
    }
}
