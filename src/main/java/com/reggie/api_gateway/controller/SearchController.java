package com.reggie.api_gateway.controller;

import com.reggie.api_gateway.dto.IngestRequest;
import com.reggie.api_gateway.dto.SearchRequest;
import com.reggie.api_gateway.index.BruteForceIndex;
import com.reggie.api_gateway.service.SearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    public List<BruteForceIndex.SearchResult> search(@RequestBody SearchRequest request) {
        return searchService.search(request.getQuery(), request.getK());
    }

    @PostMapping("/ingest")
    public String ingest(@RequestBody IngestRequest request) {
        searchService.add(request.getId(), request.getVector());
        return "Vector Added";
    }
}
