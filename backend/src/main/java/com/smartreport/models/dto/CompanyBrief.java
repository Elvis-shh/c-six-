package com.smartreport.models.dto;

import com.smartreport.models.entity.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBrief {

    private String code;
    private String name;
    private String shortName;
    private String industry;
    private String market;

    public static CompanyBrief from(Company company) {
        return CompanyBrief.builder()
                .code(company.getCode())
                .name(company.getName())
                .shortName(company.getShortName())
                .industry(company.getIndustry())
                .market(company.getMarket())
                .build();
    }
}
