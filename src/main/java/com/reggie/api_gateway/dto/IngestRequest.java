package com.reggie.api_gateway.dto;

public class IngestRequest {
    private int id;
    private float[] vector;

    public IngestRequest() {

    }

    public int getId() {
        return id;
    }

    public float[] getVector() {
        return vector;
    }
}
