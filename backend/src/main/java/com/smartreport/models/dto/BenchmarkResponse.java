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
public class BenchmarkResponse {

    private String industry;
    private List<IndicatorBenchmark> indicators;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorBenchmark {
        private String key;
        private String name;
        private BigDecimal companyValue;
        private BigDecimal industryAvg;
        private BigDecimal industryMedian;
        private String rank;
        private String rankLabel;
        private String unit;
    }
}
