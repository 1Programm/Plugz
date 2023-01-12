package com.programm.plugz.persist.aqua;

import com.programm.plugz.persist.ex.PersistQueryBuildException;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/*
person.find(*)                                          -> find all from a table called person
person.find({a,b,c})                                    -> find all entries but only return attributes a, b and c
person.find(*, id = 1)                                  -> find all entries where id equals 1
person.find(*, age > 10)                                -> find all entries where age is greater than 10
person.find({name}, id != 1 & age <= 20)                -> find all entries where id does not equal 1 and age is less than or equal to 20 but only return the attribute name
person.find(*, age = 10 | id = 2)                       -> find all entires where age equals 10 or id equals 2
person.find(*, (name = 'Artur' & age > 10) | id < 3)    -> find all entries where name equals Artur and age is greater than 10 or where id is less than 3

person.find(*, name = ''Ar.*'')                         -> find all entries where name matches the pattern Ar.* (-> Arthur, Arima, Ar...)





todo
ADVANCED:
person.find(*, id = neighbor.find({personId}, age = 10))

person.find(*) + ...


*/

@RequiredArgsConstructor
class AquaQueryBuilder {

    private static class Index {
        public int i;

        public void advanceWhitespace(String s){
            i = AquaQueryBuilder.advanceWhitespace(s, i);
        }
    }

    private static int advanceWhitespace(String s, int i){
        while(i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static int regressWhitespace(String s, int i){
        while(i >= 0 && Character.isWhitespace(s.charAt(i-1))) i--;
        return i;
    }

    public QueryExecutionInfo parseQuery(String query) throws PersistQueryBuildException {
        try {
            return parseQuery(query, new Index());
        }
        catch (PersistQueryBuildException e){
            throw new PersistQueryBuildException("Failed to parse query: [" + query + "].", e);
        }
    }

    private QueryExecutionInfo parseQuery(String s, Index pos) throws PersistQueryBuildException {
        //Term starts with name of table and a dot after which a method is called
        int nextDot = s.indexOf('.', pos.i);
        if(nextDot == -1) throw new PersistQueryBuildException("Invalid Term: Expected a '.' after index: [" + pos + "]");

        //The name of the table to work on
        String tableName = s.substring(pos.i, nextDot);

        //Method has a named always followed by an open round Bracket
        int nextOpenRoundBracket = s.indexOf('(', nextDot + 1);
        if(nextOpenRoundBracket == -1) throw new PersistQueryBuildException("Invalid Term: Expected a '(' after index: [" + (nextDot + 1) + "]");

        //The Method to execute
        String methodName = s.substring(nextDot + 1, nextOpenRoundBracket);


        pos.i = nextOpenRoundBracket;
        if(methodName.equals("find")){
            return createFindTerm(s, pos, tableName);
        }

        throw new PersistQueryBuildException("Invalid Term: Unknown Method name [" + methodName + "]");
    }

    private QueryExecutionInfo createFindTerm(String s, Index pos, String tableName) throws PersistQueryBuildException {
        pos.i++;
        List<String> selections = createSelections(s, pos);
        QueryTerm conditionTerm = null;

        pos.advanceWhitespace(s);
        if(!s.startsWith(")", pos.i)) {
            if (!s.startsWith(",", pos.i)) throw new PersistQueryBuildException("Invalid Term: Expected a ',' at index: [" + pos.i + "]");
            pos.i++;

            conditionTerm = createConditionTerm(s, pos);
            pos.advanceWhitespace(s);
            //if(!s.startsWith(")", pos.i)) throw new PersistQueryBuildException("Invalid Term: Expected a closing ')' at index: [" + pos.i + "]");
        }

        return new QueryExecutionInfo(tableName, "find", selections, conditionTerm);
    }

    private List<String> createSelections(String s, Index pos) throws PersistQueryBuildException {
        if(s.startsWith("*", pos.i)){
            pos.i++;
            return null;
        }
        else if(s.startsWith("{", pos.i)){
            int nextClosingCurlyBracket = s.indexOf('}', pos.i + 1);
            if(nextClosingCurlyBracket == -1) throw new PersistQueryBuildException("Invalid Term: Expected closing '}' after index [" + (pos.i + 1) + "]");
            if(nextClosingCurlyBracket == pos.i + 1) throw new PersistQueryBuildException("Invalid Term: Expected some param names for selection.");

            String _parameters = s.substring(pos.i + 1, nextClosingCurlyBracket);
            String[] parameters = _parameters.split(",");

            List<String> selections = new ArrayList<>();
            for(String param : parameters){
                param = param.trim();
                if(param.isEmpty()) throw new PersistQueryBuildException("Invalid Term: Empty parameter name for selection");
                selections.add(param);
            }

            pos.i = nextClosingCurlyBracket + 1;
            return selections;
        }

        throw new PersistQueryBuildException("Invalid Term: Expected a '*' or '{' for selection.");
    }

    private QueryTerm createConditionTerm(String s, Index pos) throws PersistQueryBuildException {
        int i;
        for(i=s.length()-1;i>=pos.i;i--){
            char c = s.charAt(i);
            if(Character.isWhitespace(c)) continue;
            if(c == ')') break;
            if(i == pos.i) throw new PersistQueryBuildException("No closing ')' bracket at end of term!");
        }

        QueryTerm term = createConditionTerm(s, pos.i, i);
        pos.i = i;
        return term;
    }

    private QueryTerm createConditionTerm(String s, int start, int end) throws PersistQueryBuildException {
        int _start = start, _end = end;
        start = advanceWhitespace(s, start);
        end = regressWhitespace(s, end);
        if(end <= start) throw new PersistQueryBuildException("Empty condition term at [" + _start + " - " + _end + "]");
//        String tmp = s.substring(start, end);

        int nextOp = findNextConnectionOpOutsideBrackets(s, start, end);

        if(nextOp != -1){
            String op = "" + s.charAt(nextOp);

            QueryTerm left = parseBasicTerm(s, start, nextOp);
            QueryTerm right = parseBasicTerm(s, nextOp + op.length(), end);

            return new QueryTerm.ConnectionTerm(op, left, right);
        }
        else if(s.startsWith("(", start) && s.startsWith(")", end - 1)){
            return createConditionTerm(s, start + 1, end - 1);
        }
        else {
            //Smallest Term
            //=, !=, >, <, >=, <=

            int opPos = -1;
            String op = null;
            for(int i=start;i<end;i++){
                if(s.startsWith("=", i) || s.startsWith(">", i) || s.startsWith("<", i)){
                    opPos = i;
                    op = s.substring(i, i + 1);
                    break;
                }
                else if(s.startsWith("!=", i) || s.startsWith(">=", i) || s.startsWith("<=", i)){
                    opPos = i;
                    op = s.substring(i, i + 2);
                    break;
                }
            }

            if(opPos == -1) throw new PersistQueryBuildException("Simple term must contain an operator from index: [" + start + " - " + end + "]");

            QueryTerm left = parseBasicTerm(s, start, opPos);
            QueryTerm right = parseBasicTerm(s, opPos + op.length(), end);

            return new QueryTerm.OperationTerm(op, left, right);
        }
    }

    private QueryTerm parseBasicTerm(String s, int start, int end) throws PersistQueryBuildException {
        int _start = start, _end = end;
        start = advanceWhitespace(s, start);
        end = regressWhitespace(s, end);
        if(end <= start) throw new PersistQueryBuildException("Empty basic term at [" + _start + " - " + _end + "]");
//        String tmp = s.substring(start, end);

        if(s.startsWith("'", start) && s.startsWith("'", end-1)){
            start++; end--;
            boolean regex = false;
            if(s.startsWith("'", start) && s.startsWith("'", end-1)){
                start++; end--;
                regex = true;
            }

            String value = s.substring(start, end);
            return new QueryTerm.ValueTerm(value, regex);
        }
        else if(s.startsWith("$", start)){
            String varName = s.substring(start + 1, end);
            return new QueryTerm.VarTerm(varName);
        }

        String value = s.substring(start, end);
        if(value.equalsIgnoreCase("true")){
            return new QueryTerm.ValueTerm(true, false);
        }
        else if(value.equalsIgnoreCase("false")){
            return new QueryTerm.ValueTerm(false, false);
        }

        try {
            double doubleValue = Double.parseDouble(value);
            return new QueryTerm.ValueTerm(doubleValue, false);
        }
        catch (NumberFormatException ignore){}

        return new QueryTerm.ColumnNameTerm(value);
    }

    private int findNextConnectionOpOutsideBrackets(String s, int start, int end) throws PersistQueryBuildException {
        int openBrackets = 0;

        for(int i=start;i<end;i++){
            char c = s.charAt(i);
            if(c == '('){
                openBrackets++;
            }
            else if(c == ')'){
                openBrackets--;
                if(openBrackets < 0) throw new PersistQueryBuildException("Illegal closing bracket ')' before opening bracket at index: [" + i + "].");
            }
            else if(openBrackets == 0 && (c == '&' || c == '|')){
                return i;
            }
        }

        if(openBrackets != 0) throw new PersistQueryBuildException("Illegal open bracket/s without closing bracket/s after index: [" + start + "].");
        return -1;
    }

}
