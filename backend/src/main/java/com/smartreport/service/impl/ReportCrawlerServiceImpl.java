package com.smartreport.service.impl;

import com.smartreport.models.dto.CrawlerDtos.*;
import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.CompanyIndustryTag;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.models.entity.ReportCrawlTask;
import com.smartreport.models.entity.ReportFile;
import com.smartreport.repository.CompanyIndustryTagRepository;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.repository.ReportCrawlTaskRepository;
import com.smartreport.repository.ReportFileRepository;
import com.smartreport.service.ReportCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCrawlerServiceImpl implements ReportCrawlerService {
    private final CompanyRepository companyRepository;
    private final CompanyIndustryTagRepository companyIndustryTagRepository;
    private final ReportCrawlTaskRepository taskRepository;
    private final FinancialReportRepository reportRepository;
    private final ReportFileRepository reportFileRepository;

    @Value("${ai-engine.url:http://localhost:8000}")
    private String aiEngineUrl;

    @Override
    public CrawlStartResponse start(CrawlStartRequest request) {
        List<Company> companies = loadCompanies(request);

        int created = 0;
        int skipped = 0;
        for (Company company : companies) {
            for (int year = request.getStartYear(); year <= request.getEndYear(); year++) {
                for (String reportType : request.getReportTypes()) {
                    if (taskRepository.findByCompanyCodeAndReportYearAndReportType(company.getCode(), year, reportType).isPresent()) {
                        skipped++;
                        continue;
                    }
                    ReportCrawlTask task = ReportCrawlTask.builder()
                            .companyCode(company.getCode())
                            .reportYear(year)
                            .reportType(reportType)
                            .status("pending")
                            .build();
                    if (isBeforeListingYear(company, year)) {
                        task.setStatus("skipped");
                        task.setErrorMsg("上市前年份，跳过抓取");
                        task.setCompletedAt(LocalDateTime.now());
                    }
                    taskRepository.save(task);
                    created++;
                }
            }
        }
        return CrawlStartResponse.builder().created(created).skipped(skipped).totalCandidates(companies.size()).build();
    }

    private List<Company> loadCompanies(CrawlStartRequest request) {
        int limit = request.getLimit() == null ? 30 : request.getLimit();
        if ("CSI300".equalsIgnoreCase(request.getIndexCode()) || "000300".equals(request.getIndexCode())
                || "CSI_A50".equalsIgnoreCase(request.getIndexCode()) || "A50".equalsIgnoreCase(request.getIndexCode()) || "000510".equals(request.getIndexCode())) {
            boolean csiA50 = "CSI_A50".equalsIgnoreCase(request.getIndexCode()) || "A50".equalsIgnoreCase(request.getIndexCode()) || "000510".equals(request.getIndexCode());
            List<AiIndexConstituent> constituents = fetchIndexConstituents(csiA50);
            List<Company> companies = new ArrayList<>();
            for (AiIndexConstituent item : constituents.stream().limit(limit).toList()) {
                Company company = companyRepository.findById(item.getCode()).orElseGet(() -> companyRepository.save(Company.builder()
                        .code(item.getCode())
                        .name(item.getName())
                        .shortName(item.getName())
                        .market(item.getMarket())
                        .industry(item.getIndustry() != null && !item.getIndustry().isBlank() ? item.getIndustry() : (csiA50 ? "中证A50" : "沪深300"))
                        .status(1)
                        .build()));
                if (company.getListingDate() == null && item.getListingDate() != null) {
                    company.setListingDate(LocalDate.parse(item.getListingDate()));
                }
                if (item.getIndustry() != null && !item.getIndustry().isBlank()) {
                    company.setIndustry(item.getIndustry());
                }
                if (company.getListingDate() == null && item.getListingDate() != null || item.getIndustry() != null && !item.getIndustry().isBlank()) {
                    company = companyRepository.save(company);
                }
                if (csiA50 && !companyIndustryTagRepository.existsByCompanyCodeAndTag(company.getCode(), "中证A50")) {
                    companyIndustryTagRepository.save(CompanyIndustryTag.builder()
                            .companyCode(company.getCode())
                            .tag("中证A50")
                            .build());
                }
                companies.add(company);
            }
            return companies;
        }
        return companyRepository.findByStatus(1).stream()
                .filter(company -> List.of("SH", "SZ").contains(company.getMarket()))
                .limit(limit)
                .toList();
    }

    private List<AiIndexConstituent> fetchIndexConstituents(boolean csiA50) {
        RestClient client = RestClient.builder().baseUrl(aiEngineUrl).build();
        AiIndexResponse response = client.get()
                .uri(csiA50 ? "/ai/v1/indices/csi-a50/constituents" : "/ai/v1/indices/csi300/constituents")
                .retrieve()
                .body(AiIndexResponse.class);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new IllegalStateException((csiA50 ? "中证A50" : "沪深300") + "成分股获取失败");
        }
        return response.getData();
    }

    @Override
    public CrawlerStatusResponse status() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of("pending", "processing", "completed", "failed", "skipped")) {
            counts.put(status, taskRepository.countByStatus(status));
        }
        return CrawlerStatusResponse.builder()
                .counts(counts)
                .recentTasks(taskRepository.findTop50ByOrderByCreatedAtDesc())
                .build();
    }

    @Async("crawlerExecutor")
    @Override
    public void runPendingAsync() {
        List<ReportCrawlTask> pending = taskRepository.findAll().stream()
                .filter(task -> "pending".equals(task.getStatus()))
                .toList();
        List<ReportCrawlTask> priorityTasks = pending.stream()
                .filter(task -> task.getReportYear() != null && task.getReportYear() >= 2021 && task.getReportYear() <= 2025)
                .filter(task -> companyIndustryTagRepository.existsByCompanyCodeAndTag(task.getCompanyCode(), "中证A50"))
                .limit(100)
                .toList();
        List<ReportCrawlTask> tasks = priorityTasks.isEmpty()
                ? pending.stream().limit(100).toList()
                : priorityTasks;
        for (ReportCrawlTask task : tasks) {
            crawlOne(task);
            sleepQuietly(600L);
        }
    }

    private void crawlOne(ReportCrawlTask task) {
        Company company = companyRepository.findById(task.getCompanyCode()).orElse(null);
        if (isBeforeListingYear(company, task.getReportYear())) {
            task.setStatus("skipped");
            task.setErrorMsg("上市前年份，跳过抓取");
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
            return;
        }

        task.setStatus("processing");
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        try {
            RestClient client = RestClient.builder().baseUrl(aiEngineUrl).build();
            AiFetchReportResponse response = client.post()
                    .uri("/ai/v1/reports/fetch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AiFetchReportRequest.builder()
                            .companyCode(task.getCompanyCode())
                            .year(task.getReportYear())
                            .reportType(task.getReportType())
                            .build())
                    .retrieve()
                    .body(AiFetchReportResponse.class);
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("AI 引擎没有返回报告数据");
            }
            saveReport(task, response.getData());
        } catch (Exception e) {
            task.setStatus("failed");
            task.setRetryCount(task.getRetryCount() + 1);
            task.setErrorMsg(e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
            log.warn("crawl report failed: {} {} {}", task.getCompanyCode(), task.getReportYear(), task.getReportType(), e);
        }
    }

    private void saveReport(ReportCrawlTask task, AiFetchReportData data) {
        FinancialReport report = reportRepository
                .findByCompanyCodeAndReportYearAndReportType(data.getCompanyCode(), data.getReportYear(), data.getReportType())
                .orElseGet(() -> FinancialReport.builder()
                        .companyCode(data.getCompanyCode())
                        .reportYear(data.getReportYear())
                        .reportType(data.getReportType())
                        .source("crawler")
                        .status(1)
                        .build());
        report.setSourceFileUrl(data.getFilePath());
        if (data.getPublishedAt() != null) {
            report.setPublishedAt(LocalDate.parse(data.getPublishedAt()));
        }
        report = reportRepository.save(report);

        reportFileRepository.save(ReportFile.builder()
                .reportId(report.getId())
                .fileName(data.getFileName())
                .fileUrl(data.getFilePath())
                .fileType(data.getFileType())
                .fileSize(data.getFileSize())
                .build());

        task.setStatus("completed");
        task.setAnnouncementTitle(data.getTitle());
        task.setSourceUrl(data.getSourceUrl());
        task.setFilePath(data.getFilePath());
        task.setFileName(data.getFileName());
        task.setFileSize(data.getFileSize());
        if (data.getPublishedAt() != null) {
            task.setPublishedAt(LocalDate.parse(data.getPublishedAt()));
        }
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isBeforeListingYear(Company company, Integer reportYear) {
        return company != null
                && company.getListingDate() != null
                && reportYear != null
                && reportYear < company.getListingDate().getYear();
    }
}
