package com.smartreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartreport.models.dto.UploadDtos.ConfirmExtractionRequest;
import com.smartreport.models.dto.UploadDtos.ExtractedIndicator;
import com.smartreport.models.dto.UploadDtos.ProgressRequest;
import com.smartreport.models.dto.UploadDtos.TaskStatusResponse;
import com.smartreport.models.dto.UploadDtos.UploadTaskResponse;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.models.entity.MqTaskRecord;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.repository.MqTaskRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final FileValidationService fileValidationService;
    private final MqTaskRecordRepository taskRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final ObjectMapper objectMapper;

    public UploadTaskResponse upload(MultipartFile file) throws IOException {
        String ext = fileValidationService.validate(file);
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "fileName", safeFileName(file),
                "fileType", ext,
                "fileSize", file.getSize()
        );
        MqTaskRecord task = MqTaskRecord.builder()
                .taskId(taskId)
                .taskType("report_upload")
                .status("processing")
                .progressMsg("文件已上传，等待解析")
                .progressPercent(10)
                .requestPayload(objectMapper.writeValueAsString(payload))
                .build();
        taskRepository.save(task);
        simulateExtraction(taskId, safeFileName(file), file.getBytes());
        return UploadTaskResponse.builder()
                .taskId(taskId)
                .status(task.getStatus())
                .fileName(safeFileName(file))
                .message("文件已上传，正在排队解析...")
                .build();
    }

    private void simulateExtraction(String taskId, String fileName, byte[] content) throws JsonProcessingException {
        String text = new String(content);
        Map<String, ExtractedIndicator> extracted = Map.of(
                "revenue", new ExtractedIndicator(1748D, "亿", 0.92D, "mock", "营业收入"),
                "profit", new ExtractedIndicator(862D, "亿", 0.9D, "mock", "净利润"),
                "cashFlow", new ExtractedIndicator(810D, "亿", 0.88D, "mock", "经营现金流"),
                "grossMargin", new ExtractedIndicator(92.8D, "%", 0.86D, "mock", "毛利率")
        );
        MqTaskRecord task = getTask(taskId);
        task.setProgressMsg((text.isBlank() ? fileName : "已读取上传内容") + "，等待用户确认提取结果");
        task.setProgressPercent(85);
        task.setResponsePayload(objectMapper.writeValueAsString(extracted));
        taskRepository.save(task);
    }

    public TaskStatusResponse getStatus(String taskId) throws JsonProcessingException {
        MqTaskRecord task = getTask(taskId);
        Map<String, ExtractedIndicator> extracted = null;
        if (task.getResponsePayload() != null && !task.getResponsePayload().isBlank()) {
            extracted = objectMapper.readValue(task.getResponsePayload(), new TypeReference<>() {});
        }
        return TaskStatusResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .stage(stageOf(task.getProgressPercent()))
                .message(task.getProgressMsg())
                .percent(task.getProgressPercent())
                .extractedData(extracted)
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
        FinancialReport report = reportRepository.save(FinancialReport.builder()
                .companyCode(request.getCompanyCode())
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
    }

    private MqTaskRecord getTask(String taskId) {
        return taskRepository.findByTaskId(taskId).orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));
    }

    private String safeFileName(MultipartFile file) {
        return file.getOriginalFilename() == null ? "report" : file.getOriginalFilename();
    }

    private String stageOf(Integer percent) {
        if (percent == null || percent < 30) return "upload";
        if (percent < 70) return "ocr";
        if (percent < 100) return "ner";
        return "done";
    }
}
