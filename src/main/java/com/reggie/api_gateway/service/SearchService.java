package com.reggie.api_gateway.service;

import com.reggie.api_gateway.index.BruteForceIndex;
import com.reggie.api_gateway.index.VectorIndex;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {
    private VectorIndex index = new BruteForceIndex();

    public List<BruteForceIndex.SearchResult> search(float[] query, int k) {
        return index.search(query, k);
    }

    public void add(int id, float[] vector) {
        index.add(id, vector);
    }
}
