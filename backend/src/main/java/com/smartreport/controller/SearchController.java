package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.models.dto.CompanyBrief;
import com.smartreport.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/companies")
    public ApiResponse<List<CompanyBrief>> searchCompanies(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int limit) {
        List<CompanyBrief> results = searchService.searchCompanies(q, limit);
        return ApiResponse.success(results);
    }

    @GetMapping("/companies/hot")
    public ApiResponse<List<CompanyBrief>> getHotCompanies() {
        return ApiResponse.success(searchService.getHotCompanies());
    }
}
