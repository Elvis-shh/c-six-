package com.smartreport.service.impl;

import com.smartreport.models.dto.ParseDtos.*;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.service.ReportParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportParseServiceImpl implements ReportParseService {
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;

    @Value("${ai-engine.url:http://localhost:8000}")
    private String aiEngineUrl;

    @Override
    public ParseStartResponse start(ParseStartRequest request) {
        List<FinancialReport> candidates = reportRepository.findAll().stream()
                .filter(report -> request.getSource().equalsIgnoreCase(report.getSource()))
                .filter(report -> report.getSourceFileUrl() != null && !report.getSourceFileUrl().isBlank())
                .filter(report -> indicatorRepository.findByReportId(report.getId()).isEmpty())
                .limit(request.getLimit() == null ? 20 : request.getLimit())
                .toList();
        runPendingAsyncInternal(candidates);
        return ParseStartResponse.builder().queued(candidates.size()).skipped(0).build();
    }

    @Async("crawlerExecutor")
    @Override
    public void runPendingAsync() {
        List<FinancialReport> candidates = reportRepository.findAll().stream()
                .filter(report -> report.getSourceFileUrl() != null && !report.getSourceFileUrl().isBlank())
                .filter(report -> indicatorRepository.findByReportId(report.getId()).isEmpty())
                .limit(20)
                .toList();
        runPendingAsyncInternal(candidates);
    }

    private void runPendingAsyncInternal(List<FinancialReport> candidates) {
        for (FinancialReport report : candidates) {
            parseOne(report);
            sleepQuietly(1200L);
        }
    }

    @Transactional
    protected void parseOne(FinancialReport report) {
        RestClient client = RestClient.builder().baseUrl(aiEngineUrl).build();
        AiParseReportResponse response = client.post()
                .uri("/ai/v1/reports/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiParseReportRequest(report.getSourceFileUrl()))
                .retrieve()
                .body(AiParseReportResponse.class);
        if (response == null || response.getData() == null || response.getData().getExtractedData() == null) {
            return;
        }

        indicatorRepository.deleteByReportId(report.getId());
        response.getData().getExtractedData().forEach((key, value) -> {
            if (value == null || value.getValue() == null) {
                return;
            }
            indicatorRepository.save(FinancialIndicator.builder()
                    .reportId(report.getId())
                    .indicatorKey(key)
                    .value(BigDecimal.valueOf(value.getValue()))
                    .rating(value.getConfidence() != null && value.getConfidence() >= 0.8 ? "good" : "warning")
                    .build());
        });
    }

    @Override
    public ParseStatusResponse status() {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<FinancialReport> reports = reportRepository.findAll();
        counts.put("parsed", reports.stream().filter(report -> !indicatorRepository.findByReportId(report.getId()).isEmpty()).count());
        counts.put("unparsed", reports.stream().filter(report -> indicatorRepository.findByReportId(report.getId()).isEmpty()).count());
        return ParseStatusResponse.builder()
                .recentReports(reports.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).limit(50).toList())
                .counts(counts)
                .build();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
