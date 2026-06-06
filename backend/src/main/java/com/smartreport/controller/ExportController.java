package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.models.entity.ExportRecord;
import com.smartreport.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @PostMapping
    public ApiResponse<Map<String, Object>> submitExport(@RequestBody Map<String, String> body) {
        String companyCode = body.getOrDefault("companyCode", "unknown");
        String format = body.getOrDefault("format", "pdf");
        String taskId = exportService.submit(companyCode, format);
        return ApiResponse.success(Map.of("taskId", taskId, "status", "pending"));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        ExportRecord record = exportService.getTaskStatus(taskId);
        return ApiResponse.success(Map.of(
                "taskId", record.getTaskId(),
                "status", record.getStatus(),
                "progress", record.getProgress(),
                "fileName", record.getFileName() != null ? record.getFileName() : ""
        ));
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> download(@PathVariable String taskId) throws Exception {
        ExportRecord record = exportService.getCompletedTask(taskId);
        byte[] bytes = exportService.downloadBytes(taskId);
        ByteArrayResource resource = new ByteArrayResource(bytes);

        MediaType mediaType = switch (record.getFormat()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "doc" -> MediaType.parseMediaType("application/msword");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + record.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}
