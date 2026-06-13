package com.reggie.api_gateway.dto;

public class SearchRequest {
    private float[] query;
    private int k;

    public SearchRequest() {

    }

    public float[] getQuery() {
        return query;
    }

    public int getK() {
        return k;
    }
}
