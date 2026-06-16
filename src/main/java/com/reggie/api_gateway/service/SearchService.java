package com.reggie.api_gateway.service;

import com.reggie.api_gateway.dto.VectorRecord;
import com.reggie.api_gateway.index.BruteForceIndex;
import com.reggie.api_gateway.index.VectorIndex;
import com.reggie.api_gateway.repository.VectorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SearchService {
    private VectorIndex index = new BruteForceIndex();
    private VectorRepository repository;
    private RedisTemplate<String, Object> redis;

    public SearchService(VectorRepository repository, RedisTemplate<String, Object> redis) {
        this.repository = repository;
        this.redis = redis;
    }

    public List<BruteForceIndex.SearchResult> search(float[] query, int k) {
        String key = "search:" + Arrays.hashCode(query);
        Object cached = redis.opsForValue().get(key);
        if (cached != null) {
            return (List<BruteForceIndex.SearchResult>) cached;
        } else {
            List<BruteForceIndex.SearchResult> result = index.search(query, k);
            redis.opsForValue().set(key, result);
            return result;
        }
    }

    public void add(int id, String label, float[] vector) {
        index.add(id, vector);
        repository.save(id, label, vector);
    }

    @PostConstruct
    public void loadFromDatabase() {
        List<VectorRecord> records = repository.findAll();
        for (VectorRecord record : records) {
            index.add(record.getId(), record.getVector());
        }
    }
}
