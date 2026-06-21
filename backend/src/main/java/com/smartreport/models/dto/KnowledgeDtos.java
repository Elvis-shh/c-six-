package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class KnowledgeDtos {
    @Data
    public static class BuildKnowledgeRequest {
        private String indexCode = "CSI_A50";
        private Integer limit = 200;
        private Boolean includeWeb = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuildKnowledgeResponse {
        private Integer imported;
        private Integer skipped;
    }

    @Data
    public static class KnowledgeSearchRequest {
        private String query;
        private String companyCode;
        private Integer topK = 8;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeSearchItem {
        private Long id;
        private String sourceType;
        private String sourceName;
        private String sourceUrl;
        private String companyCode;
        private Integer pageNo;
        private String content;
        private Double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeSearchResponse {
        private List<KnowledgeSearchItem> results;
    }
}
