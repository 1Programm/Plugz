package com.programm.plugz.codegen;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

class JavaSourceCodeGenerator {

    public static final String PROXY_FIELD_HANDLER = "$handler";

    public static String generateJavaClassProxy(String packageName, String className, Class<?> superClass, String visibility){
        StringBuilder sb = new StringBuilder();

        if(!packageName.isEmpty()) sb.append("package ").append(packageName).append(";\n");
        sb.append(visibility);
        if(!visibility.isEmpty()) sb.append(" ");
        sb.append("class ").append(className);

        sb.append(" extends ").append(superClass.getName()).append(" {\n");

        sb.append("public ").append(ProxyMethodHandler.class.getName()).append(" ").append(PROXY_FIELD_HANDLER).append(";\n");


        Set<String> alreadyGeneratedMethods = new HashSet<>();

        Method[] declaredMethods = superClass.getDeclaredMethods();
        for(Method method : declaredMethods){
            alreadyGeneratedMethods.add(method.toGenericString());
            appendMethod(sb, method, packageName, className, superClass);
        }

        Method[] methods = superClass.getMethods();
        for(Method method : methods){
            String gmName = method.toGenericString();
            if(alreadyGeneratedMethods.contains(gmName)) continue;
            appendMethod(sb, method, packageName, className, superClass);
        }


        sb.append("}");


        return sb.toString();
    }

    private static void appendMethod(StringBuilder sb, Method method, String packageName, String className, Class<?> superClass){
        int modifiers = method.getModifiers();
        if(Modifier.isFinal(modifiers) || Modifier.isNative(modifiers)) return;

        String _modifiers = Modifier.toString(modifiers);
        Class<?> returnType = method.getReturnType();
        String _returnType = returnType.getName();
        String methodName = method.getName();

        sb.append(_modifiers).append(" ").append(_returnType).append(" ").append(methodName).append("(");

        Class<?>[] methodParameterTypes = method.getParameterTypes();
        StringBuilder argsNamesAsInput = new StringBuilder();
        StringBuilder argsTypesAsInput = new StringBuilder();
        for(int i=0;i<methodParameterTypes.length;i++){
            if(i != 0) {
                sb.append(", ");
                argsNamesAsInput.append(", ");
                argsTypesAsInput.append(", ");
            }

            Class<?> methodParameterType = methodParameterTypes[i];
            sb.append(methodParameterType.getName());
            sb.append(" v").append(i);
            argsNamesAsInput.append("v").append(i);


            argsTypesAsInput.append(methodParameterType.getName()).append(".class");
        }

        sb.append(")");

        Class<?>[] methodExceptionTypes = method.getExceptionTypes();
        if(methodExceptionTypes.length != 0){
            sb.append(" throws ");
            for(int i=0;i<methodExceptionTypes.length;i++){
                if(i != 0) sb.append(", ");
                Class<?> methodExceptionType = methodExceptionTypes[i];
                String _methodExceptionType = methodExceptionType.getName();
                sb.append(_methodExceptionType);
            }
        }

        sb.append(" {\n");

        sb.append("\t").append(Method.class.getName()).append(" m;\n");
        sb.append("\ttry {\n");
        sb.append("\t\t").append("m = ").append(superClass.getName()).append(".class.getMethod(").append("\"").append(methodName).append("\"");
        if(!argsTypesAsInput.isEmpty()){
            sb.append(", ").append(argsTypesAsInput);
        }
        sb.append(");\n");
        sb.append("\t} catch(").append(NoSuchMethodException.class.getName()).append(" e) {\n\t\tthrow new ").append(IllegalStateException.class.getName()).append("(\"ILLEGAL STATE: Method should exist!\", e);\n\t}\n");
        sb.append("\tif(").append(PROXY_FIELD_HANDLER).append(".canHandle(this, m)){\n");

//        sb.append("\t\t").append(Method.class.getName()).append(" mOrig;\n");
//        sb.append("\t\ttry {\n");
//        sb.append("\t\t\t").append("mOrig = ").append(superClass.getName()).append(".class.getMethod(").append("\"").append(methodName).append("\"");
//        if(!argsTypesAsInput.isEmpty()){
//            sb.append(", ").append(argsTypesAsInput);
//        }
//        sb.append(");\n");
//        sb.append("\t\t} catch(").append(NoSuchMethodException.class.getName()).append(" e) {\n\t\t\tthrow new ").append(IllegalStateException.class.getName()).append("(\"ILLEGAL STATE: Method should exist!\", e);\n\t\t}\n");

        sb.append("\t\ttry {\n");
        sb.append("\t\t\t");
        if(returnType != Void.TYPE){
            sb.append("return (").append(_returnType).append(") ");
        }
        sb.append(PROXY_FIELD_HANDLER).append(".invoke(this, ").append(GenerateCodeHelper.class.getName()).append(".wrapMethod(").append(superClass.getName()).append(".class").append(", ");
        if(!packageName.isEmpty()) sb.append(packageName).append(".");
        sb.append(className).append(".class").append(", m)");

        if(!argsNamesAsInput.isEmpty()){
            sb.append(", ").append(argsNamesAsInput);
        }

        sb.append(");\n");
        sb.append("\t\t} catch (").append(Exception.class.getName()).append(" e) {\n\t\t\tthrow new ").append(ProxyClassRuntimeException.class.getName()).append("(e);\n\t\t}\n");

        sb.append("\t} else {\n\t\t");
        if(returnType != Void.TYPE){
            sb.append("return ");
        }
        sb.append("super.").append(methodName).append("(").append(argsNamesAsInput).append(");\n");
        sb.append("\t}\n");


        sb.append("}\n");
    }


}
