package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class ParseDtos {
    @Data
    public static class ParseStartRequest {
        private Integer limit = 20;
        private String source = "crawler";
        private String indexCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseStartResponse {
        private Integer queued;
        private Integer skipped;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseStatusResponse {
        private List<?> recentReports;
        private Map<String, Long> counts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiParseReportRequest {
        private String filePath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiParseReportResponse {
        private Integer code;
        private String message;
        private AiParseReportData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiParseReportData {
        private String filePath;
        private Integer pageCount;
        private Integer textLength;
        private Map<String, AiExtractedIndicator> extractedData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiQuoteChunk {
        private Integer page;
        private String source;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiQuoteResponse {
        private Integer code;
        private String message;
        private List<AiQuoteChunk> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiExtractedIndicator {
        private Double value;
        private String unit;
        private Double confidence;
        private String method;
        private String matchedText;
    }
}
