package com.reggie.api_gateway.service;

import com.reggie.api_gateway.dto.VectorRecord;
import com.reggie.api_gateway.index.BruteForceIndex;
import com.reggie.api_gateway.index.VectorIndex;
import com.reggie.api_gateway.repository.VectorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    private VectorIndex index = new BruteForceIndex();
    private VectorRepository repository;

    public SearchService(VectorRepository repository) {
        this.repository = repository;
    }

    public List<BruteForceIndex.SearchResult> search(float[] query, int k) {
        return index.search(query, k);
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
