package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiItem {

    private String key;
    private String name;
    private BigDecimal value;
    private String unit;
    private BigDecimal yoy;
    private String trend;  // up / down / down_good
}
