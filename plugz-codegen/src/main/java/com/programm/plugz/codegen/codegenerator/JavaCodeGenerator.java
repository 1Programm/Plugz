package com.programm.plugz.codegen.codegenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Class to generate Java Code and build it as a string.
 * Most complex statements still have to be written by hand.
 */
public class JavaCodeGenerator {

    private static final int S_END_CLASS                    = -1;
    private static final int S_NO_CLASS                     = 0;
    private static final int S_DEFINING_CLASS               = 1;
    private static final int S_IN_CLASS                     = 2;
    private static final int S_DEFINING_INNER_CLASS         = 3;
    private static final int S_IN_INNER_CLASS               = 4;
    private static final int S_DEFINING_CONSTRUCTOR         = 5;
    private static final int S_IN_CONSTRUCTOR               = 6;
    private static final int S_DEFINING_METHOD              = 7;
    private static final int S_IN_METHOD                    = 8;
    private static final int S_DEFINING_STATEMENT           = 9;
    private static final int S_IN_STATEMENT                 = 10;


    private static final int S__DEFINE_ONLY_IMPLEMENTS      = 0;
    private static final int S__THROWS_DECLARATION_STARTED  = 1;

    private static boolean s_oneof(int checkState, int... states){
        for(int state : states) if(checkState == state) return true;
        return false;
    }


    //TMP INFO -- START
    private final StringBuilder sb = new StringBuilder(100);
    private final Stack<String> classNameStack = new Stack<>();
    private final Stack<Integer> stateStack = new Stack<>();
    private int state = S_NO_CLASS;
    private int subState = -1;
    private int tabs = 0;
    private boolean packageDefined = false;
    //TMP INFO -- END

    private void tabs(){
        sb.append("\t".repeat(tabs));
    }

    /**
     * Defines a package in the java class.
     * Can only be invoked once!
     * @param packageName the java package name
     * @throws JavaCodeGenerationException if package name was already defined or this method was not the first to be called.
     */
    public void definePackage(String packageName) throws JavaCodeGenerationException {
        if(packageName == null || packageName.isEmpty()) throw new JavaCodeGenerationException("Package name cannot be null or empty!", sb);
        if(state != S_NO_CLASS) throw new JavaCodeGenerationException("Package declaration can only happen once at the start!", sb);
        if(packageDefined) throw new JavaCodeGenerationException("Cannot define multiple package declarations!", sb);

        sb.append("package ").append(packageName).append(";\n\n");
        packageDefined = true;
    }

    /**
     * Defines a java class or inner class.
     * @param className the class name
     * @param classType the class type
     * @param visibility the visibility
     * @param modifiers other modifiers
     * @throws JavaCodeGenerationException if in an invalid state (while defining a method body, ...)
     */
    public void defineClass(String className, ClassType classType, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_NO_CLASS, S_IN_CLASS, S_IN_INNER_CLASS)) throw new JavaCodeGenerationException("Cannot define a class at this point!", sb);

        tabs();
        visibility.withSpace(sb);

        Set<Modifier> alreadyUsedModifiers = new HashSet<>();
        for(Modifier modifier : modifiers) {
            if(alreadyUsedModifiers.contains(modifier)) throw new JavaCodeGenerationException("Multiple of the same modifier [" + modifier + "]!", sb);
            else if(modifier == Modifier.NATIVE) throw new JavaCodeGenerationException("Class cannot be native!", sb);
            alreadyUsedModifiers.add(modifier);
            if(alreadyUsedModifiers.contains(Modifier.FINAL) && alreadyUsedModifiers.contains(Modifier.ABSTRACT)) throw new JavaCodeGenerationException("Class cannot be final and abstract!", sb);

            sb.append(modifier).append(" ");
        }

        sb.append(classType).append(" ").append(className);

        classNameStack.push(className);
        state = S_DEFINING_CLASS;
    }

    /**
     * Defines a java class or inner class.
     * Default public visibility and no modifiers.
     * @param className the class name
     * @param classType the class type
     * @throws JavaCodeGenerationException if in an invalid state (while defining a method body, ...)
     */
    public void defineClass(String className, ClassType classType) throws JavaCodeGenerationException {
        defineClass(className, classType, Visibility.PUBLIC);
    }

    /**
     * Defines a java class or inner class.
     * Default class-classtype, public-visibility and no modifiers.
     * @param className the class name
     * @throws JavaCodeGenerationException if in an invalid state (while defining a method body, ...)
     */
    public void defineClass(String className) throws JavaCodeGenerationException {
        defineClass(className, ClassType.CLASS);
    }


    /**
     * Defines a superclass for the current class.
     * @param fullSuperClassName fully quallified superclass name.
     * @throws JavaCodeGenerationException if not defining a class or inner class.
     */
    public void defineExtends(String fullSuperClassName) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_DEFINING_CLASS, S_DEFINING_INNER_CLASS) || s_oneof(subState, S__DEFINE_ONLY_IMPLEMENTS)) throw new JavaCodeGenerationException("Cannot define extends at this point!", sb);

        sb.append(" extends ").append(fullSuperClassName);
        subState = S__DEFINE_ONLY_IMPLEMENTS;
    }

    /**
     * Defines a superclass for the current class.
     * @param cls superclass.
     * @throws JavaCodeGenerationException if not defining a class or inner class.
     */
    public void defineExtends(Class<?> cls) throws JavaCodeGenerationException {
        defineExtends(cls.getName());
    }


    /**
     * Defines an implementing interface for the current class.
     * @param fullInterfaceName fully quallified interface class name.
     * @throws JavaCodeGenerationException if not defining a class or inner class.
     */
    public void defineImplements(String fullInterfaceName) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_DEFINING_CLASS, S_DEFINING_INNER_CLASS)) throw new JavaCodeGenerationException("Cannot define implements at this point!", sb);

        if(subState == S__DEFINE_ONLY_IMPLEMENTS) sb.append(", ");
        else sb.append(" implements ");
        sb.append(fullInterfaceName);

        subState = S__DEFINE_ONLY_IMPLEMENTS;
    }

    /**
     * Defines an implementing interface for the current class.
     * @param cls interface class.
     * @throws JavaCodeGenerationException if not defining a class or inner class.
     */
    public void defineImplements(Class<?> cls) throws JavaCodeGenerationException {
        defineImplements(cls.getName());
    }


    /**
     * Defines a block start (A '{' in java).
     * This must be called to start a class-body, constructor-body or a method-body.
     * @throws JavaCodeGenerationException if before or after main-class defined by this generator.
     */
    public void startBlock() throws JavaCodeGenerationException {
        if(s_oneof(state, S_END_CLASS, S_NO_CLASS)) throw new JavaCodeGenerationException("Cannot define block-start at this point!", sb);

        if(sb.charAt(sb.length() - 1) == '\n') tabs();
        else sb.append(" ");
        sb.append("{\n");

        tabs++;

        subState = -1;
        if(state == S_DEFINING_CLASS) {
            state = S_IN_CLASS;
        }
        else if(state == S_DEFINING_INNER_CLASS) {
            state = S_IN_INNER_CLASS;
        }
        else if(state == S_DEFINING_CONSTRUCTOR){
            state = S_IN_CONSTRUCTOR;
        }
        else if(state == S_DEFINING_METHOD){
            state = S_IN_METHOD;
        }
        else if(state == S_DEFINING_STATEMENT){
            state = S_IN_STATEMENT;
        }
        else {
            //TODO
            throw new JavaCodeGenerationException("NOT IMPLEMENTED YET!", sb);
        }

        stateStack.push(state);
    }


    /**
     * Defines a block end (A '}' in java).
     * This must be called to close a class, constructor-body, method-body or statement-body.
     * @throws JavaCodeGenerationException if not in a class, inner-class, constructor-body, method-body or statement-body.
     */
    public void endBlock() throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CLASS, S_IN_INNER_CLASS, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define block-stop at this point!", sb);

        tabs--;
        tabs();
        sb.append("}\n");

        stateStack.pop();

        if(state == S_IN_CLASS) {
            state = S_END_CLASS;
            classNameStack.pop();
        }
        else if(state == S_IN_INNER_CLASS) {
            subState = -1;
            classNameStack.pop();
            state = classNameStack.size() > 1 ? S_IN_INNER_CLASS : S_IN_CLASS;
        }
        else if(state == S_IN_CONSTRUCTOR || state == S_IN_METHOD){
            state = classNameStack.size() > 1 ? S_IN_INNER_CLASS : S_IN_CLASS;
        }
        else if(state == S_IN_STATEMENT){
            state = stateStack.peek();
        }
    }




    private void _defineMember(String memberName, String fullTypeName, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CLASS, S_IN_INNER_CLASS)) throw new JavaCodeGenerationException("Cannot define member at this point!", sb);

        tabs();
        visibility.withSpace(sb);

        Set<Modifier> alreadyUsedModifiers = new HashSet<>();
        for(Modifier modifier : modifiers) {
            if(alreadyUsedModifiers.contains(modifier)) throw new JavaCodeGenerationException("Multiple of the same modifier [" + modifier + "]!", sb);
            else if(modifier == Modifier.NATIVE) throw new JavaCodeGenerationException("Member cannot be native!", sb);
            else if(modifier == Modifier.ABSTRACT) throw new JavaCodeGenerationException("Member cannot be abstract!", sb);

            alreadyUsedModifiers.add(modifier);
            sb.append(modifier).append(" ");
        }

        sb.append(fullTypeName).append(" ").append(memberName);
    }

    /**
     * Defines a member for the current class.
     * @param memberName the member name.
     * @param fullTypeName the fully quallified member type name.
     * @param visibility the visibility for this member.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMember(String memberName, String fullTypeName, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        _defineMember(memberName, fullTypeName, visibility, modifiers);
        sb.append(";\n");
    }

    /**
     * Defines a member for the current class.
     * @param memberName the member name.
     * @param type the member type.
     * @param visibility the visibility for this member.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMember(String memberName, Class<?> type, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        defineMember(memberName, type.getName(), visibility, modifiers);
    }

    /**
     * Defines a member for the current class and assigns it some value.
     * @param memberName the member name.
     * @param fullTypeName the fully quallified member type name.
     * @param value the value to assign.
     * @param visibility the visibility for this member.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMemberWithValue(String memberName, String fullTypeName, String value, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        _defineMember(memberName, fullTypeName, visibility, modifiers);
        sb.append(" = ").append(value).append(";\n");
    }

    /**
     * Defines a member for the current class and assigns it some value.
     * @param memberName the member name.
     * @param type the member type.
     * @param value the value to assign.
     * @param visibility the visibility for this member.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMemberWithValue(String memberName, Class<?> type, String value, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        defineMemberWithValue(memberName, type.getName(), value, visibility, modifiers);
    }


    /**
     * Defines a constructor for the current class.
     * Parameters will automatically be named "v0", "v1", ..., "vN" for N parameters.
     * @param visibility the visibility for the constructor.
     * @param fullNameParameters the fully quallified parameter type names.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineConstructor(Visibility visibility, String... fullNameParameters) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CLASS, S_IN_INNER_CLASS)) throw new JavaCodeGenerationException("Cannot define constructor at this point!", sb);

        tabs();
        visibility.withSpace(sb);

        String className = classNameStack.peek();
        sb.append(className);

        sb.append("(");
        for(int i=0;i<fullNameParameters.length;i++){
            if(i != 0) sb.append(", ");
            sb.append(fullNameParameters[i]).append(" v").append(i);
        }
        sb.append(")");

        state = S_DEFINING_CONSTRUCTOR;
    }

    /**
     * Defines a constructor for the current class.
     * Parameters will automatically be named "v0", "v1", ..., "vN" for N parameters.
     * @param visibility the visibility for the constructor.
     * @param parameterTypes the parameter types.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineConstructor(Visibility visibility, Class<?>[] parameterTypes) throws JavaCodeGenerationException {
        String[] fullNameParameters = new String[parameterTypes.length];
        for(int i=0;i<parameterTypes.length;i++) fullNameParameters[i] = parameterTypes[i].getName();
        defineConstructor(visibility, fullNameParameters);
    }

    /**
     * Defines a constructor for the current class.
     * Parameters will automatically be named "v0", "v1", ..., "vN" for N parameters.
     * Default public-visibility.
     * @param parameterTypes the parameter types.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineConstructor(Class<?>... parameterTypes) throws JavaCodeGenerationException {
        defineConstructor(Visibility.PUBLIC, parameterTypes);
    }


    /**
     * Defines a method for the current class.
     * @param methodName the method name.
     * @param fullNameReturnType the fully quallified return type name.
     * @param fullNameParameters the fully quallified parameter type names.
     * @param visibility the visibility for this method.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMethod(String methodName, String fullNameReturnType, String[] fullNameParameters, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CLASS, S_IN_INNER_CLASS)) throw new JavaCodeGenerationException("Cannot define method at this point!", sb);

        tabs();
        visibility.withSpace(sb);

        boolean isAbstract = false;

        Set<Modifier> alreadyUsedModifiers = new HashSet<>();
        for(Modifier modifier : modifiers) {
            if(alreadyUsedModifiers.contains(modifier)) throw new JavaCodeGenerationException("Multiple of the same modifier [" + modifier + "]!", sb);
            else if(modifier == Modifier.ABSTRACT) {
                if(modifiers.length > 1) throw new JavaCodeGenerationException("Member cannot be abstract and have other modifiers!", sb);
                isAbstract = true;
            }

            alreadyUsedModifiers.add(modifier);
            sb.append(modifier).append(" ");
        }

        sb.append(fullNameReturnType).append(" ").append(methodName);
        sb.append("(");

        for(int i=0;i<fullNameParameters.length;i++){
            if(i != 0) sb.append(", ");
            sb.append(fullNameParameters[i]).append(" v").append(i);
        }
        sb.append(")");

        if(isAbstract){
            sb.append(";\n");
        }
        else {
            state = S_DEFINING_METHOD;
        }
    }

    /**
     * Defines a method for the current class.
     * @param methodName the method name.
     * @param returnType the return type.
     * @param parameterTypes the parameter types.
     * @param visibility the visibility for this method.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMethod(String methodName, Class<?> returnType, Class<?>[] parameterTypes, Visibility visibility, Modifier... modifiers) throws JavaCodeGenerationException {
        String[] fullNameParameters = new String[parameterTypes.length];
        for(int i=0;i<parameterTypes.length;i++) fullNameParameters[i] = parameterTypes[i].getName();
        defineMethod(methodName, returnType.getName(), fullNameParameters, visibility, modifiers);
    }

    /**
     * Defines a method for the current class.
     * Default public-visibility and no modifiers.
     * @param methodName the method name.
     * @param fullNameReturnType the fully quallified return type name.
     * @param fullNameParameters the fully quallified parameter type names.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMethod(String methodName, String fullNameReturnType, String... fullNameParameters) throws JavaCodeGenerationException {
        defineMethod(methodName, fullNameReturnType, fullNameParameters, Visibility.PUBLIC);
    }

    /**
     * Defines a method for the current class.
     * Default public-visibility and no modifiers.
     * @param methodName the method name.
     * @param returnType the return type.
     * @param parameterTypes the parameter types.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineMethod(String methodName, Class<?> returnType, Class<?>... parameterTypes) throws JavaCodeGenerationException {
        String[] fullNameParameters = new String[parameterTypes.length];
        for(int i=0;i<parameterTypes.length;i++) fullNameParameters[i] = parameterTypes[i].getName();
        defineMethod(methodName, returnType.getName(), fullNameParameters, Visibility.PUBLIC);
    }

    /**
     * Defines a method for the current class.
     * Default void-return-type, public-visibility and no modifiers.
     * @param methodName the method name.
     * @param parameterTypes the parameter types.
     * @throws JavaCodeGenerationException if not in a class or inner class.
     */
    public void defineVoidMethod(String methodName, Class<?>... parameterTypes) throws JavaCodeGenerationException {
        String[] fullNameParameters = new String[parameterTypes.length];
        for(int i=0;i<parameterTypes.length;i++) fullNameParameters[i] = parameterTypes[i].getName();
        defineMethod(methodName, Void.TYPE.getName(), fullNameParameters, Visibility.PUBLIC);
    }


    /**
     * Defines a throws-declaration for the current constructor or method declaration.
     * @param fullExceptionType the fully quallified exception type name.
     * @throws JavaCodeGenerationException if not defining a constructor or a method.
     */
    public void defineThrowsDeclaration(String fullExceptionType) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_DEFINING_CONSTRUCTOR, S_DEFINING_METHOD)) throw new JavaCodeGenerationException("Cannot define throws declaration at this point!", sb);

        if(subState == S__THROWS_DECLARATION_STARTED) sb.append(", ");
        else sb.append(" throws ");

        sb.append(fullExceptionType);
        subState = S__THROWS_DECLARATION_STARTED;
    }

    /**
     * Defines a throws-declaration for the current constructor or method declaration.
     * @param exceptionType the exception type.
     * @throws JavaCodeGenerationException if not defining a constructor or a method.
     */
    public void defineThrowsDeclaration(Class<?> exceptionType) throws JavaCodeGenerationException {
        defineThrowsDeclaration(exceptionType.getName());
    }




    private void _defineVariable(String variableName, String fullTypeName, Modifier... modifiers) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define variable ", sb);

        tabs();

        Set<Modifier> alreadyUsedModifiers = new HashSet<>();
        for(Modifier modifier : modifiers) {
            if(alreadyUsedModifiers.contains(modifier)) throw new JavaCodeGenerationException("Multiple of the same modifier [" + modifier + "]!", sb);
            else if(modifier == Modifier.ABSTRACT) throw new JavaCodeGenerationException("Variable cannot be abstract!", sb);
            else if(modifier == Modifier.NATIVE) throw new JavaCodeGenerationException("Variable cannot be native!", sb);
            else if(modifier == Modifier.STATIC) throw new JavaCodeGenerationException("Variable cannot be static!", sb);

            alreadyUsedModifiers.add(modifier);
            sb.append(modifier).append(" ");
        }

        sb.append(fullTypeName).append(" ").append(variableName);
    }

    /**
     * Defines a variable in the current context.
     * @param variableName the variable name.
     * @param fullTypeName the fully quallified type name.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a constructor, method, statement or a modifier other than 'final'.
     */
    public void defineVariable(String variableName, String fullTypeName, Modifier... modifiers) throws JavaCodeGenerationException {
        _defineVariable(variableName, fullTypeName, modifiers);
        sb.append(";\n");
    }

    /**
     * Defines a variable in the current context.
     * @param variableName the variable name.
     * @param type the type.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a constructor, method, statement or a modifier other than 'final'.
     */
    public void defineVariable(String variableName, Class<?> type, Modifier... modifiers) throws JavaCodeGenerationException {
        defineVariable(variableName, type.getName(), modifiers);
    }



    /**
     * Defines a variable in the current context with some value assigned to it.
     * @param variableName the variable name.
     * @param fullTypeName the fully quallified type name.
     * @param value the assigned value.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a constructor, method, statement or a modifier other than 'final'.
     */
    public void defineVariableWithAssignment(String variableName, String fullTypeName, String value, Modifier... modifiers) throws JavaCodeGenerationException {
        _defineVariable(variableName, fullTypeName, modifiers);
        sb.append(" = ").append(value).append(";\n");
    }

    /**
     * Defines a variable in the current context with some value assigned to it.
     * @param variableName the variable name.
     * @param type the type.
     * @param value the assigned value.
     * @param modifiers the modifiers.
     * @throws JavaCodeGenerationException if not in a constructor, method, statement or a modifier other than 'final'.
     */
    public void defineVariableWithAssignment(String variableName, Class<?> type, String value, Modifier... modifiers) throws JavaCodeGenerationException {
        defineVariableWithAssignment(variableName, type.getName(), value, modifiers);
    }


    /**
     * Defines an if-statement.
     * @param condition the condition.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineIfStatement(String condition) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define if-statement at this point!", sb);
        tabs();
        sb.append("if(").append(condition).append(")");
        state = S_DEFINING_STATEMENT;
    }

    /**
     * Defines an else-statement.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineElseStatement() throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define else-statement at this point!", sb);
        tabs();
        sb.append("else");
        state = S_DEFINING_STATEMENT;
    }

    /**
     * Defines an if-else-statement.
     * @param condition the condition.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineElseIfStatement(String condition) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define elseif-statement at this point!", sb);
        tabs();
        sb.append("else if(").append(condition).append(")");
        state = S_DEFINING_STATEMENT;
    }

    /**
     * Defines a try-statement.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineTryStatement() throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define try-statement at this point!", sb);
        tabs();
        sb.append("try");
        state = S_DEFINING_STATEMENT;
    }

    /**
     * Defines a catch-statement.
     * @param exceptionTypes the fully quallified exception type names.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineCatchStatement(String... exceptionTypes) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define catch-statement at this point!", sb);
        if(exceptionTypes.length == 0) throw new JavaCodeGenerationException("Needs at least one exception for catching!", sb);
        tabs();
        sb.append("catch(");

        for(int i=0;i<exceptionTypes.length;i++){
            if(i != 0) sb.append(" | ");
            sb.append(exceptionTypes[i]);
        }

        sb.append(" e");

        sb.append(")");
        state = S_DEFINING_STATEMENT;
    }

    /**
     * Defines a catch-statement.
     * @param exceptionTypes the exception types.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineCatchStatement(Class<?>... exceptionTypes) throws JavaCodeGenerationException {
        String[] exceptionTypeNames = new String[exceptionTypes.length];
        for(int i=0;i<exceptionTypes.length;i++) exceptionTypeNames[i] = exceptionTypes[i].getName();
        defineCatchStatement(exceptionTypeNames);
    }

    /**
     * Defines a for-statement.
     * @param forStatement for statement.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineForStatement(String forStatement) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define for-statement at this point!", sb);
        tabs();
        sb.append("for(").append(forStatement).append(")");
        state = S_DEFINING_STATEMENT;
    }

    /**
     * Defines a while-statement.
     * @param condition the condition.
     * @throws JavaCodeGenerationException if not in a constructor, method or statement.
     */
    public void defineWhileStatement(String condition) throws JavaCodeGenerationException {
        if(!s_oneof(state, S_IN_CONSTRUCTOR, S_IN_METHOD, S_IN_STATEMENT)) throw new JavaCodeGenerationException("Cannot define while-statement at this point!", sb);
        tabs();
        sb.append("while(").append(condition).append(")");
        state = S_DEFINING_STATEMENT;
    }



    /**
     * Defines a statement.
     * @param statement the statement.
     */
    public void defineStatement(String statement){
        tabs();
        sb.append(statement).append("\n");
    }


    /**
     * Builds the java code to string and resets this generator for a new usage.
     * @return the java code as string.
     * @throws JavaCodeGenerationException if the class was not properly completed.
     */
    public String build() throws JavaCodeGenerationException {
        if(state != S_END_CLASS) throw new JavaCodeGenerationException("Class not completed!", sb);

        String res = sb.toString();
        sb.setLength(0);
        classNameStack.clear();
        stateStack.clear();
        state = S_NO_CLASS;
        subState = -1;
        tabs = 0;
        packageDefined = false;
        return res;
    }

}
