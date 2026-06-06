package com.smartreport.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiResponse {

    private CompanyBrief company;
    private Integer reportYear;
    private String reportType;
    private List<KpiItem> kpis;
}
