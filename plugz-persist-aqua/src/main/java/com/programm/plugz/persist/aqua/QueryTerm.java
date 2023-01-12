package com.programm.plugz.persist.aqua;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

public interface QueryTerm {

    @RequiredArgsConstructor
    class ConnectionTerm implements QueryTerm {
        public final String connector;
        public final QueryTerm left;
        public final QueryTerm right;

        @Override
        public String toString() {
            return left + " " + connector + " " + right;
        }
    }

    @RequiredArgsConstructor
    class OperationTerm implements QueryTerm {
        public final String op;
        public final QueryTerm left;
        public final QueryTerm right;

        @Override
        public String toString() {
            return left + " " + op + " " + right;
        }
    }

    @RequiredArgsConstructor
    class ValueTerm implements QueryTerm {
        public final Object value;
        public final boolean regex;

        @Override
        public String toString() {
            String s = (value instanceof String) ? "'" + value + "'" : Objects.toString(value);
            return regex ? ("'" + s + "'"): s;
        }
    }

    @RequiredArgsConstructor
    class VarTerm implements QueryTerm {
        public final String name;

        @Override
        public String toString() {
            return "$" + name;
        }
    }

    @RequiredArgsConstructor
    class ColumnNameTerm implements QueryTerm {
        public final String name;

        @Override
        public String toString() {
            return name;
        }
    }

}
