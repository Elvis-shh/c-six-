package com.smartreport.service;

import com.smartreport.models.entity.ExportRecord;
import com.smartreport.repository.ExportRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExportRecordRepository exportRecordRepository;
    private final Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), "smartreport-exports");

    public String submit(String companyCode, String format) {
        String taskId = "export_" + UUID.randomUUID().toString().substring(0, 12);
        String date = LocalDateTime.now().toLocalDate().toString();
        String fileName = companyCode + "_财报分析报告_" + date + "." + format;

        ExportRecord record = ExportRecord.builder()
                .taskId(taskId)
                .companyCode(companyCode)
                .format(format)
                .status("pending")
                .fileName(fileName)
                .progress(0)
                .build();
        exportRecordRepository.save(record);

        processExport(record);
        return taskId;
    }

    @Async
    public void processExport(ExportRecord record) {
        try {
            record.setStatus("rendering");
            record.setProgress(30);
            exportRecordRepository.save(record);

            Files.createDirectories(exportDir);
            Path filePath = exportDir.resolve(record.getFileName());

            // Generate a simple HTML report as export content
            byte[] fileBytes = generatePlaceholderExport(record);
            Files.write(filePath, fileBytes);

            record.setStatus("ready");
            record.setProgress(100);
            record.setFileUrl(filePath.toAbsolutePath().toString());
            record.setFileSize((long) fileBytes.length);
            record.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Export failed: {}", record.getTaskId(), e);
            record.setStatus("failed");
            record.setErrorMsg(e.getMessage());
        }
        exportRecordRepository.save(record);
    }

    private byte[] generatePlaceholderExport(ExportRecord record) {
        // Placeholder: would be replaced with real report generation (html2pdf via Flying Saucer, etc.)
        String content = "<html><head><meta charset='utf-8'/><title>财报分析报告</title></head>" +
                "<body><h1>财报分析报告</h1>" +
                "<p>公司代码: " + record.getCompanyCode() + "</p>" +
                "<p>导出格式: " + record.getFormat() + "</p>" +
                "<p>生成时间: " + LocalDateTime.now() + "</p>" +
                "<hr/><p style='color:#92400e'>免责声明：本报告由 SmartReport 自动生成，仅供学习参考，不构成任何投资建议。</p>" +
                "</body></html>";
        return content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public ExportRecord getTaskStatus(String taskId) {
        return exportRecordRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
    }

    public ExportRecord getCompletedTask(String taskId) {
        ExportRecord record = getTaskStatus(taskId);
        if (!"ready".equals(record.getStatus())) {
            throw new IllegalStateException("Export not ready, current status: " + record.getStatus());
        }
        return record;
    }

    public byte[] downloadBytes(String taskId) throws IOException {
        ExportRecord record = getCompletedTask(taskId);
        return Files.readAllBytes(Paths.get(record.getFileUrl()));
    }
}
