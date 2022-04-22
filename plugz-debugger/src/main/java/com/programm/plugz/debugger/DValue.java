package com.programm.plugz.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DValue<T> {

    public static class Bool extends DValue<Boolean> {
        public Bool() {
            this(false);
        }

        public Bool(Boolean value) {
            super(value);
        }

        public void toggle(){
            set(!get());
        }
    }

    public static class Int extends DValue<Integer> {
        public Int() {
            this(0);
        }

        public Int(Integer value) {
            super(value);
        }

        public void add(int i){
            set(value + i);
        }

        public void sub(int i){
            set(value - i);
        }

        public void increment(){
            add(1);
        }

        public void decrement(){
            sub(1);
        }
    }

    public static class Long extends DValue<java.lang.Long> {
        public Long() {
            this(0L);
        }

        public Long(java.lang.Long value) {
            super(value);
        }

        public void add(long i){
            set(value + i);
        }

        public void sub(long i){
            set(value - i);
        }

        public void increment(){
            add(1);
        }

        public void decrement(){
            sub(1);
        }
    }

    public static class Float extends DValue<java.lang.Float> {
        public Float() {
            this(0f);
        }

        public Float(java.lang.Float value) {
            super(value);
        }

        public void add(float i){
            set(value + i);
        }

        public void sub(float i){
            set(value - i);
        }

        public void increment(){
            add(1);
        }

        public void decrement(){
            sub(1);
        }
    }

    public static class Double extends DValue<java.lang.Double> {
        public Double() {
            this(0d);
        }

        public Double(java.lang.Double value) {
            super(value);
        }

        public void add(double i){
            set(value + i);
        }

        public void sub(double i){
            set(value - i);
        }

        public void increment(){
            add(1);
        }

        public void decrement(){
            sub(1);
        }
    }




    private final List<Runnable> changeListeners = new ArrayList<>();
    protected T value;

    public DValue(){}

    public DValue(T value){
        this.value = value;
    }

    public void set(T value){
        this.value = value;
        for(int i=0;i<changeListeners.size();i++) changeListeners.get(i).run();
    }

    public T get(){
        return value;
    }

    public void addChangeListener(Runnable runnable){
        changeListeners.add(runnable);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }
}
