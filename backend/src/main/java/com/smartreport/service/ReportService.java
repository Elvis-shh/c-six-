package com.smartreport.service;

import com.smartreport.models.dto.IndicatorDetailResponse;
import com.smartreport.models.dto.KpiResponse;
import com.smartreport.models.dto.TimelineResponse;

import java.util.List;

public interface ReportService {

    KpiResponse getKpi(String companyCode);

    TimelineResponse getTimeline(String companyCode, List<String> metricKeys);

    KpiResponse getLatestReport(String companyCode);

    IndicatorDetailResponse getIndicators(String companyCode);
}
