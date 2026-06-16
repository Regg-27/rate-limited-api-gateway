package com.reggie.api_gateway.index;


import com.reggie.api_gateway.math.VectorMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class BruteForceIndex implements VectorIndex {
    private List<Integer> ids = new ArrayList<>();
    private List<float[]> vectors = new ArrayList<>();

    public void add(int id, float[] vector) {
        ids.add(id);
        vectors.add(vector);
    }

    public List<SearchResult> search(float[] query, int k) {
        PriorityQueue<SearchResult> minHeap = new PriorityQueue<>((a, b) -> Float.compare(a.score, b.score));

        for (int i = 0; i < vectors.size(); i++) {
            int id = ids.get(i);
            float score = VectorMath.cosine(query, vectors.get(i));
            if (minHeap.size() < k) {
                minHeap.offer(new SearchResult(id, score));
            } else if (score > minHeap.peek().score) {
                minHeap.poll();
                minHeap.offer(new SearchResult(id, score));
            }
        }
        List<SearchResult> results = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            results.add(minHeap.poll());
        }
        Collections.reverse(results);
        return results;
    }

    public int size() {
        return vectors.size();
    }

    public static class SearchResult {
        public int id;
        public float score;

        public SearchResult(int id, float score) {
            this.id = id;
            this.score = score;
        }

        public SearchResult() {

        }
    }
}

