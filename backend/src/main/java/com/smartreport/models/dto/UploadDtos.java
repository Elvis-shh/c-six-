package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

public class UploadDtos {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadTaskResponse {
        private String taskId;
        private String status;
        private String fileName;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatusResponse {
        private String taskId;
        private String status;
        private String stage;
        private String message;
        private Integer percent;
        private Map<String, ExtractedIndicator> extractedData;
        private String companyCode;
        private String companyName;
        private Integer reportYear;
        private String industry;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressRequest {
        private String stage;
        private String message;
        private Integer percent;
        private String status;
        private Map<String, ExtractedIndicator> extractedData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmExtractionRequest {
        private String companyCode;
        private String companyName;
        private String industry;
        private Integer reportYear;
        private Map<String, ExtractedIndicator> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedIndicator {
        private Double value;
        private String unit;
        private Double confidence;
        private String method;
        private String matchedText;
    }
}
