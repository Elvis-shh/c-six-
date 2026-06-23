package com.smartreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartreport.models.dto.ParseDtos.AiParseReportResponse;
import com.smartreport.models.dto.ParseDtos.AiParseReportData;
import com.smartreport.models.dto.UploadDtos.ConfirmExtractionRequest;
import com.smartreport.models.dto.UploadDtos.ExtractedIndicator;
import com.smartreport.models.dto.UploadDtos.ProgressRequest;
import com.smartreport.models.dto.UploadDtos.TaskStatusResponse;
import com.smartreport.models.dto.UploadDtos.UploadTaskResponse;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.models.entity.MqTaskRecord;
import com.smartreport.models.entity.Company;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.repository.MqTaskRecordRepository;
import com.smartreport.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final FileValidationService fileValidationService;
    private final MqTaskRecordRepository taskRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final CompanyRepository companyRepository;
    private final AiEngineClient aiEngineClient;
    private final ObjectMapper objectMapper;

    private static final Path UPLOAD_DIR = Path.of("/app/data/uploads");
    private static final Map<String, String> crawlProgress = new LinkedHashMap<>();

    public UploadTaskResponse upload(MultipartFile file) throws IOException {
        String ext = fileValidationService.validate(file);
        String taskId = UUID.randomUUID().toString();
        String fileName = safeFileName(file);

        // Save file to disk so AI engine can access it
        Files.createDirectories(UPLOAD_DIR);
        Path filePath = UPLOAD_DIR.resolve(taskId + "_" + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> payload = Map.of(
                "fileName", fileName,
                "fileType", ext,
                "fileSize", file.getSize(),
                "filePath", filePath.toString()
        );
        MqTaskRecord task = MqTaskRecord.builder()
                .taskId(taskId)
                .taskType("report_upload")
                .status("processing")
                .progressMsg("文件已上传，正在调用AI解析...")
                .progressPercent(20)
                .requestPayload(objectMapper.writeValueAsString(payload))
                .build();
        taskRepository.save(task);

        // Call AI engine to parse the uploaded PDF
        try {
            AiParseReportResponse parseResult = aiEngineClient.parseReport(filePath.toString());
            if (parseResult != null && parseResult.getData() != null && parseResult.getData().getExtractedData() != null) {
                AiParseReportData data = parseResult.getData();
                Map<String, ExtractedIndicator> extracted = convertExtracted(parseResult);
                task.setProgressMsg("AI解析完成，请确认提取结果");
                task.setProgressPercent(85);
                Map<String, Object> responsePayload = new LinkedHashMap<>();
                responsePayload.put("extractedData", extracted);
                responsePayload.put("companyCode", data.getCompanyCode());
                responsePayload.put("companyName", data.getCompanyName());
                responsePayload.put("reportYear", data.getReportYear());
                responsePayload.put("industry", data.getIndustry());
                task.setResponsePayload(objectMapper.writeValueAsString(responsePayload));
            } else {
                task.setProgressMsg("AI未能从文件中提取到指标数据，请确认文件是否为标准财报PDF");
                task.setProgressPercent(50);
            }
        } catch (Exception e) {
            task.setProgressMsg("AI解析失败: " + e.getMessage());
            task.setProgressPercent(30);
        }
        taskRepository.save(task);

        return UploadTaskResponse.builder()
                .taskId(taskId)
                .status(task.getStatus())
                .fileName(fileName)
                .message(task.getProgressMsg())
                .build();
    }

    private Map<String, ExtractedIndicator> convertExtracted(AiParseReportResponse parseResult) {
        Map<String, ExtractedIndicator> result = new LinkedHashMap<>();
        parseResult.getData().getExtractedData().forEach((key, ind) -> {
            if (ind.getValue() != null) {
                result.put(key, new ExtractedIndicator(
                        ind.getValue(),
                        ind.getUnit() != null ? ind.getUnit() : "",
                        ind.getConfidence(),
                        ind.getMethod(),
                        ind.getMatchedText()
                ));
            }
        });
        return result;
    }

    public TaskStatusResponse getStatus(String taskId) throws JsonProcessingException {
        MqTaskRecord task = getTask(taskId);
        Map<String, ExtractedIndicator> extracted = null;
        String companyCode = null;
        String companyName = null;
        Integer reportYear = null;
        String industry = null;
        if (task.getResponsePayload() != null && !task.getResponsePayload().isBlank()) {
            Map<String, Object> payload = objectMapper.readValue(task.getResponsePayload(), new TypeReference<>() {});
            if (payload.containsKey("extractedData")) {
                extracted = objectMapper.convertValue(payload.get("extractedData"), new TypeReference<>() {});
            } else {
                extracted = objectMapper.convertValue(payload, new TypeReference<>() {});
            }
            companyCode = (String) payload.get("companyCode");
            companyName = (String) payload.get("companyName");
            if (payload.get("reportYear") instanceof Integer) {
                reportYear = (Integer) payload.get("reportYear");
            }
            industry = (String) payload.get("industry");
        }
        return TaskStatusResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .stage(stageOf(task.getProgressPercent()))
                .message(task.getProgressMsg())
                .percent(task.getProgressPercent())
                .extractedData(extracted)
                .companyCode(companyCode)
                .companyName(companyName)
                .reportYear(reportYear)
                .industry(industry)
                .build();
    }

    public void updateProgress(String taskId, ProgressRequest request) throws JsonProcessingException {
        MqTaskRecord task = getTask(taskId);
        task.setProgressMsg(request.getMessage());
        task.setProgressPercent(request.getPercent() == null ? task.getProgressPercent() : request.getPercent());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getExtractedData() != null) {
            task.setResponsePayload(objectMapper.writeValueAsString(request.getExtractedData()));
        }
        if ("completed".equals(task.getStatus()) || "failed".equals(task.getStatus())) {
            task.setCompletedAt(LocalDateTime.now());
        }
        taskRepository.save(task);
    }

    public void confirm(String taskId, ConfirmExtractionRequest request) {
        MqTaskRecord task = getTask(taskId);
        String companyCode = request.getCompanyCode();
        String industry = request.getIndustry();

        // Auto-register company if not exists
        if (!companyRepository.existsById(companyCode)) {
            Company company = Company.builder()
                    .code(companyCode)
                    .name(request.getCompanyName() != null ? request.getCompanyName() : companyCode)
                    .shortName(request.getCompanyName())
                    .industry(industry != null ? industry : "其他行业")
                    .market(detectMarket(companyCode))
                    .status(1)
                    .build();
            companyRepository.save(company);
        }

        // Delete existing upload report for this company+year before creating new
        reportRepository.findByCompanyCodeAndReportYearAndReportType(companyCode, request.getReportYear(), "annual")
                .filter(r -> "upload".equals(r.getSource()))
                .ifPresent(existing -> {
                    indicatorRepository.deleteByReportId(existing.getId());
                    reportRepository.delete(existing);
                });

        FinancialReport report = reportRepository.save(FinancialReport.builder()
                .companyCode(companyCode)
                .reportType("annual")
                .reportYear(request.getReportYear())
                .source("upload")
                .status(1)
                .build());
        request.getData().forEach((key, value) -> {
            if (value != null && value.getValue() != null) {
                indicatorRepository.save(FinancialIndicator.builder()
                        .reportId(report.getId())
                        .indicatorKey(key)
                        .value(BigDecimal.valueOf(value.getValue()))
                        .build());
            }
        });
        task.setStatus("completed");
        task.setProgressMsg("数据已确认并写入报表");
        task.setProgressPercent(100);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);

        // Async: crawl last 5 years of reports for this company
        crawlProgress.put(companyCode, "正在爬取近5年年报...");
        new Thread(() -> {
            try {
                Map<String, Object> batchResult = aiEngineClient.batchFetchParse(companyCode, 5);
                List<Map<String, Object>> results = (List<Map<String, Object>>) batchResult.get("data");
                if (results != null) {
                    int done = 0;
                    for (Map<String, Object> item : results) {
                        if (!"ok".equals(item.get("status"))) {
                            done++;
                            continue;
                        }
                        int year = ((Number) item.get("year")).intValue();
                        String filePath = (String) item.get("filePath");
                        Map<String, Object> indicators = (Map<String, Object>) item.get("indicators");
                        if (indicators == null || indicators.isEmpty()) { done++; continue; }

                        if (reportRepository.existsByCompanyCodeAndReportYearAndSource(companyCode, year, "crawler")) {
                            done++; continue;
                        }

                        // Delete any existing report for this year (uploaded or stale)
                        reportRepository.findByCompanyCodeAndReportYearAndReportType(companyCode, year, "annual")
                                .ifPresent(existing -> {
                                    indicatorRepository.deleteByReportId(existing.getId());
                                    reportRepository.delete(existing);
                                });

                        FinancialReport crawledReport = reportRepository.save(FinancialReport.builder()
                                .companyCode(companyCode)
                                .reportType("annual")
                                .reportYear(year)
                                .source("crawler")
                                .sourceFileUrl(filePath)
                                .status(1)
                                .build());

                        for (Map.Entry<String, Object> entry : indicators.entrySet()) {
                            Map<String, Object> ind = (Map<String, Object>) entry.getValue();
                            if (ind == null || ind.get("value") == null) continue;
                            Double confidence = ind.get("confidence") instanceof Number ? ((Number) ind.get("confidence")).doubleValue() : 1.0;
                            if (confidence < 0.8) continue;
                            indicatorRepository.save(FinancialIndicator.builder()
                                    .reportId(crawledReport.getId())
                                    .indicatorKey(entry.getKey())
                                    .value(BigDecimal.valueOf(((Number) ind.get("value")).doubleValue()))
                                    .rating(confidence >= 0.8 ? "good" : "warning")
                                    .build());
                        }
                        done++;
                        crawlProgress.put(companyCode, "已补齐 " + done + "/" + results.size() + " 年 (" + year + ")");
                    }
                    crawlProgress.put(companyCode, "done:" + done);
                } else {
                    crawlProgress.put(companyCode, "failed:无返回数据");
                }
            } catch (Exception e) {
                crawlProgress.put(companyCode, "failed:" + e.getMessage());
            }
        }).start();
    }

    public String getCrawlProgress(String companyCode) {
        return crawlProgress.getOrDefault(companyCode, "");
    }

    private MqTaskRecord getTask(String taskId) {
        return taskRepository.findByTaskId(taskId).orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));
    }

    private String safeFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null ? name : "report.pdf";
    }

    private String stageOf(Integer percent) {
        if (percent == null || percent < 30) return "upload";
        if (percent < 70) return "parse";
        if (percent < 100) return "review";
        return "done";
    }

    private String detectMarket(String code) {
        if (code.startsWith("60") || code.startsWith("68")) return "SH";
        if (code.startsWith("00") || code.startsWith("30")) return "SZ";
        return "UNKNOWN";
    }
}
