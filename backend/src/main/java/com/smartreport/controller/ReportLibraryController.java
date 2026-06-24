package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.service.ReportLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/report-library")
@RequiredArgsConstructor
public class ReportLibraryController {
    private final ReportLibraryService reportLibraryService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(reportLibraryService.list());
    }

    @DeleteMapping("/{reportId}")
    public ApiResponse<Void> delete(@PathVariable Long reportId) {
        reportLibraryService.deleteUploadedReport(reportId);
        return ApiResponse.success(null);
    }
}
