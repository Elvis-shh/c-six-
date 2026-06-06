package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorDetailResponse {

    private List<IndicatorDetail> indicators;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorDetail {
        private String key;
        private String name;
        private String unit;
        private BigDecimal value;
        private BigDecimal yoy;
        private String trend;
        private String category;
        private String explanation;
        private String evaluation;
        private Integer sortOrder;
    }
}
