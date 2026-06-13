package com.reggie.api_gateway.index;

import java.util.List;

public interface VectorIndex {
    List<BruteForceIndex.SearchResult> search(float[] query, int k);
    void add(int id, float[] vector);
}
