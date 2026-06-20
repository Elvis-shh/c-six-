package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.models.dto.IndicatorDetailResponse;
import com.smartreport.models.dto.KpiResponse;
import com.smartreport.models.dto.TimelineResponse;
import com.smartreport.service.IndicatorService;
import com.smartreport.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final IndicatorService indicatorService;

    @GetMapping("/reports/{companyCode}/kpi")
    public ApiResponse<KpiResponse> getKpi(@PathVariable String companyCode) {
        return ApiResponse.success(reportService.getKpi(companyCode));
    }

    @GetMapping("/reports/{companyCode}/timeline")
    public ApiResponse<TimelineResponse> getTimeline(
            @PathVariable String companyCode,
            @RequestParam(required = false) String metrics) {
        List<String> metricKeys = metrics == null || metrics.isBlank()
                ? List.of()
                : Arrays.stream(metrics.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return ApiResponse.success(reportService.getTimeline(companyCode, metricKeys));
    }

    @GetMapping("/reports/{companyCode}/latest")
    public ApiResponse<KpiResponse> getLatest(@PathVariable String companyCode) {
        return ApiResponse.success(reportService.getLatestReport(companyCode));
    }

    @GetMapping("/reports/{companyCode}/indicators")
    public ApiResponse<IndicatorDetailResponse> getIndicators(@PathVariable String companyCode) {
        return ApiResponse.success(reportService.getIndicators(companyCode));
    }

    @GetMapping("/terms")
    public ApiResponse<Map<String, Map<String, String>>> getTerms() {
        return ApiResponse.success(indicatorService.getAllDefinitions());
    }
}
