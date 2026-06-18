package com.smartreport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smartreport.common.ApiResponse;
import com.smartreport.models.dto.UploadDtos.ConfirmExtractionRequest;
import com.smartreport.models.dto.UploadDtos.ProgressRequest;
import com.smartreport.models.dto.UploadDtos.TaskStatusResponse;
import com.smartreport.models.dto.UploadDtos.UploadTaskResponse;
import com.smartreport.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @PostMapping("/report")
    public ApiResponse<UploadTaskResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.accepted(uploadService.upload(file));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskStatusResponse> status(@PathVariable String taskId) throws JsonProcessingException {
        return ApiResponse.success(uploadService.getStatus(taskId));
    }

    @PostMapping("/tasks/{taskId}/progress")
    public ApiResponse<Void> progress(@PathVariable String taskId, @RequestBody ProgressRequest request) throws JsonProcessingException {
        uploadService.updateProgress(taskId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/tasks/{taskId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable String taskId, @RequestBody ConfirmExtractionRequest request) {
        uploadService.confirm(taskId, request);
        return ApiResponse.success(null);
    }
}
