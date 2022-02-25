package com.programm.projects.plugz.db.memory;

import java.util.ArrayList;
import java.util.List;

public class BTree <K extends Comparable<K>, V>{

    public static void main(String[] args) {
        BTree tree = new BTree(3);

    }

    private class Node {
        private K key;
        private final List<V> values = new ArrayList<>();

        public Node(K key, V val) {
            this.key = key;
            this.values.add(val);
        }
    }

    private final int order;
    private Node root;

    public BTree(int order) {
        this.order = order;
    }

    public void add(K key, V val){

    }

}
