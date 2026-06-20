package com.smartreport.service.impl;

import com.smartreport.models.dto.ParseDtos.*;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.models.entity.ReportQuoteChunk;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.repository.ReportQuoteChunkRepository;
import com.smartreport.service.ReportParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportParseServiceImpl implements ReportParseService {
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final ReportQuoteChunkRepository quoteChunkRepository;

    @Value("${ai-engine.url:http://localhost:8000}")
    private String aiEngineUrl;

    @Override
    public ParseStartResponse start(ParseStartRequest request) {
        List<FinancialReport> candidates = reportRepository.findAll().stream()
                .filter(report -> request.getSource().equalsIgnoreCase(report.getSource()))
                .filter(report -> report.getStatus() != null && report.getStatus() == 1)
                .filter(report -> report.getSourceFileUrl() != null && !report.getSourceFileUrl().isBlank())
                .filter(report -> indicatorRepository.findByReportId(report.getId()).isEmpty())
                .limit(normalizeLimit(request.getLimit(), 20))
                .toList();
        runPendingAsyncInternal(candidates);
        return ParseStartResponse.builder().queued(candidates.size()).skipped(0).build();
    }

    @Async("crawlerExecutor")
    @Override
    public void runPendingAsync() {
        List<FinancialReport> candidates = reportRepository.findAll().stream()
                .filter(report -> report.getSourceFileUrl() != null && !report.getSourceFileUrl().isBlank())
                .filter(report -> report.getStatus() != null && report.getStatus() == 1)
                .filter(report -> indicatorRepository.findByReportId(report.getId()).isEmpty())
                .limit(200)
                .toList();
        runPendingAsyncInternal(candidates);
    }

    @Override
    @Transactional
    public void reparse(Long reportId) {
        FinancialReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("报告不存在: " + reportId));
        parseOne(report);
    }

    private void runPendingAsyncInternal(List<FinancialReport> candidates) {
        for (FinancialReport report : candidates) {
            try {
                parseOne(report);
            } catch (Exception ignored) {
                // Bad PDFs or weak extractions should not stop the batch.
                report.setStatus(3);
                reportRepository.save(report);
            }
            sleepQuietly(200L);
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
            report.setStatus(3);
            reportRepository.save(report);
            return;
        }

        indicatorRepository.deleteByReportId(report.getId());
        int[] saved = {0};
        response.getData().getExtractedData().forEach((key, value) -> {
            if (!isUsableIndicator(key, value)) {
                return;
            }
            indicatorRepository.save(FinancialIndicator.builder()
                    .reportId(report.getId())
                    .indicatorKey(key)
                    .value(BigDecimal.valueOf(value.getValue()).setScale(4, RoundingMode.HALF_UP))
                    .rating(value.getConfidence() != null && value.getConfidence() >= 0.8 ? "good" : "warning")
                    .build());
            saved[0]++;
        });
        if (saved[0] == 0) {
            report.setStatus(2);
            reportRepository.save(report);
            return;
        }
        report.setStatus(1);
        reportRepository.save(report);

        List<FinancialIndicator> persisted = indicatorRepository.findByReportId(report.getId());
        Map<String, BigDecimal> valueMap = new LinkedHashMap<>();
        for (FinancialIndicator item : persisted) {
            valueMap.put(item.getIndicatorKey(), item.getValue());
        }
        saveDerivedIndicator(report.getId(), "debtRatio", percentage(valueMap.get("totalLiabilities"), valueMap.get("totalAssets")));
        saveDerivedIndicator(report.getId(), "netMargin", percentage(valueMap.get("profit"), valueMap.get("revenue")));
    }

    @Override
    public ParseStatusResponse status() {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<FinancialReport> reports = reportRepository.findAll();
        counts.put("parsed", reports.stream().filter(report -> !indicatorRepository.findByReportId(report.getId()).isEmpty()).count());
        counts.put("noData", reports.stream().filter(report -> report.getStatus() != null && report.getStatus() == 2).count());
        counts.put("parseFailed", reports.stream().filter(report -> report.getStatus() != null && report.getStatus() == 3).count());
        counts.put("unparsed", reports.stream()
                .filter(report -> report.getStatus() != null && report.getStatus() == 1)
                .filter(report -> indicatorRepository.findByReportId(report.getId()).isEmpty())
                .count());
        return ParseStatusResponse.builder()
                .recentReports(reports.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).limit(50).toList())
                .counts(counts)
                .build();
    }

    @Override
    @Transactional
    public ParseStartResponse importQuoteChunks(ParseStartRequest request) {
        List<FinancialReport> candidates = reportRepository.findAll().stream()
                .filter(report -> request.getSource().equalsIgnoreCase(report.getSource()))
                .filter(report -> report.getSourceFileUrl() != null && !report.getSourceFileUrl().isBlank())
                .filter(report -> !quoteChunkRepository.existsByReportId(report.getId()))
                .limit(normalizeLimit(request.getLimit(), 20))
                .toList();

        int queued = 0;
        for (FinancialReport report : candidates) {
            if (importOneQuoteChunk(report)) {
                queued++;
            }
        }
        return ParseStartResponse.builder().queued(queued).skipped(candidates.size() - queued).build();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int normalizeLimit(Integer limit, int fallback) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, 300);
    }

    private boolean isUsableIndicator(String key, AiExtractedIndicator indicator) {
        if (indicator == null || indicator.getValue() == null) {
            return false;
        }
        if (indicator.getConfidence() == null || indicator.getConfidence() < 0.8) {
            return false;
        }
        double value = indicator.getValue();
        if (!Double.isFinite(value)) {
            return false;
        }
        if ("grossMargin".equals(key)) {
            return value >= -100 && value <= 100;
        }
        if ("revenue".equals(key) || "totalAssets".equals(key)) {
            return value >= 1 && value <= 500000000;
        }
        if ("profit".equals(key) || "totalLiabilities".equals(key) || "cashFlow".equals(key)) {
            return value >= -500000000 && value <= 500000000;
        }
        return value > 0;
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private void saveDerivedIndicator(Long reportId, String key, BigDecimal value) {
        if (value == null) {
            return;
        }
        indicatorRepository.save(FinancialIndicator.builder()
                .reportId(reportId)
                .indicatorKey(key)
                .value(value)
                .rating("derived")
                .build());
    }

    private boolean importOneQuoteChunk(FinancialReport report) {
        RestClient client = RestClient.builder().baseUrl(aiEngineUrl).build();
        AiQuoteResponse response = client.post()
                .uri("/ai/v1/reports/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiParseReportRequest(report.getSourceFileUrl()))
                .retrieve()
                .body(AiQuoteResponse.class);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return false;
        }
        quoteChunkRepository.deleteByReportId(report.getId());
        for (AiQuoteChunk item : response.getData()) {
            if (item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            quoteChunkRepository.save(ReportQuoteChunk.builder()
                    .reportId(report.getId())
                    .companyCode(report.getCompanyCode())
                    .reportYear(report.getReportYear())
                    .pageNo(item.getPage() == null ? 0 : item.getPage())
                    .sourceName(item.getSource() == null ? "财报原文" : item.getSource())
                    .content(item.getContent())
                    .build());
        }
        return true;
    }
}
