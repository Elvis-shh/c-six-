package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ChatModels {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagContext {
        private String id;
        private String content;
        private String source;
        private Double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagSearchRequest {
        private String query;
        private String companyCode;
        private Integer topK;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagSearchResponse {
        private Integer code;
        private String message;
        private List<RagContext> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateRequest {
        private String companyCode;
        private String companyName;
        private String industry;
        private String message;
        private List<RagContext> ragContext;
    }
}
