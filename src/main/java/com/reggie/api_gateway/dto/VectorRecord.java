package com.reggie.api_gateway.dto;

public class VectorRecord {
    private int id;
    private String label;
    private float[] vector;

    public VectorRecord() {

    }

    public VectorRecord(int id, String label, float[] vector) {
        this.id = id;
        this.label = label;
        this.vector = vector;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public float[] getVector() {
        return vector;
    }
}
