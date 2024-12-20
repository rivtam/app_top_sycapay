package com.syca.wizzitessai;

public class IntKeyStringValue {

    private int key;
    private String value;

    // Constructor
    public IntKeyStringValue(int key, String value) {
        this.key = key;
        this.value = value;
    }

    // Getter for key
    public int getKey() {
        return key;
    }

    // Setter for key
    public void setKey(int key) {
        this.key = key;
    }

    // Getter for value
    public String getValue() {
        return value;
    }

    // Setter for value
    public void setValue(String value) {
        this.value = value;
    }

    // Override toString() method to return the value
    @Override
    public String toString() {
        return value;
    }
}

