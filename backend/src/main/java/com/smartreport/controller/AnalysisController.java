package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.models.dto.BenchmarkResponse;
import com.smartreport.service.AnalysisService;
import com.smartreport.service.BenchmarkService;
import com.smartreport.service.PredictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final BenchmarkService benchmarkService;
    private final AnalysisService analysisService;
    private final PredictService predictService;

    @GetMapping("/{companyCode}/benchmark")
    public ApiResponse<BenchmarkResponse> getBenchmark(
            @PathVariable String companyCode,
            @RequestParam(defaultValue = "2024") int year) {
        return ApiResponse.success(benchmarkService.getBenchmark(companyCode, year));
    }

    @GetMapping("/{companyCode}/highlights")
    public ApiResponse<List<Map<String, Object>>> getHighlights(@PathVariable String companyCode) {
        return ApiResponse.success(analysisService.getHighlights(companyCode));
    }

    @GetMapping("/{companyCode}/risks")
    public ApiResponse<List<Map<String, Object>>> getRisks(@PathVariable String companyCode) {
        return ApiResponse.success(analysisService.getRisks(companyCode));
    }

    @GetMapping("/{companyCode}/predict")
    public ApiResponse<Map<String, Object>> getPredict(@PathVariable String companyCode) {
        return ApiResponse.success(predictService.predict(companyCode));
    }
}
