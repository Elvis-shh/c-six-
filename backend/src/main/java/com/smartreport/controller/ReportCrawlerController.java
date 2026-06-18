package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.models.dto.CrawlerDtos.CrawlStartRequest;
import com.smartreport.models.dto.CrawlerDtos.CrawlStartResponse;
import com.smartreport.models.dto.CrawlerDtos.CrawlerStatusResponse;
import com.smartreport.service.ReportCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crawler/reports")
@RequiredArgsConstructor
public class ReportCrawlerController {
    private final ReportCrawlerService reportCrawlerService;

    @PostMapping("/start")
    public ApiResponse<CrawlStartResponse> start(@RequestBody CrawlStartRequest request) {
        return ApiResponse.accepted(reportCrawlerService.start(request));
    }

    @PostMapping("/run")
    public ApiResponse<Void> run() {
        reportCrawlerService.runPendingAsync();
        return ApiResponse.accepted(null);
    }

    @GetMapping("/status")
    public ApiResponse<CrawlerStatusResponse> status() {
        return ApiResponse.success(reportCrawlerService.status());
    }
}
