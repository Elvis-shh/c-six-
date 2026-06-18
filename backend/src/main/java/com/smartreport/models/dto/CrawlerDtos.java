package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class CrawlerDtos {
    @Data
    public static class CrawlStartRequest {
        private Integer startYear = 2024;
        private Integer endYear = 2024;
        private List<String> reportTypes = List.of("annual");
        private Integer limit = 30;
        private String indexCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrawlStartResponse {
        private Integer created;
        private Integer skipped;
        private Integer totalCandidates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrawlerStatusResponse {
        private Map<String, Long> counts;
        private List<?> recentTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiFetchReportRequest {
        private String companyCode;
        private Integer year;
        private String reportType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiFetchReportResponse {
        private Integer code;
        private String message;
        private AiFetchReportData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiFetchReportData {
        private String companyCode;
        private Integer reportYear;
        private String reportType;
        private String title;
        private String publishedAt;
        private String sourceUrl;
        private String fileName;
        private String filePath;
        private String fileType;
        private Long fileSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiIndexResponse {
        private Integer code;
        private String message;
        private List<AiIndexConstituent> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiIndexConstituent {
        private String code;
        private String name;
        private String market;
        private String source;
    }
}
