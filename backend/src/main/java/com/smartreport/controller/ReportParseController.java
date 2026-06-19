package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.models.dto.ParseDtos.ParseStartRequest;
import com.smartreport.models.dto.ParseDtos.ParseStartResponse;
import com.smartreport.models.dto.ParseDtos.ParseStatusResponse;
import com.smartreport.service.ReportParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parse/reports")
@RequiredArgsConstructor
public class ReportParseController {
    private final ReportParseService reportParseService;

    @PostMapping("/start")
    public ApiResponse<ParseStartResponse> start(@RequestBody ParseStartRequest request) {
        return ApiResponse.accepted(reportParseService.start(request));
    }

    @PostMapping("/run")
    public ApiResponse<Void> run() {
        reportParseService.runPendingAsync();
        return ApiResponse.accepted(null);
    }

    @GetMapping("/status")
    public ApiResponse<ParseStatusResponse> status() {
        return ApiResponse.success(reportParseService.status());
    }
}
