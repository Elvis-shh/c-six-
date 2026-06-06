package com.smartreport.service.impl;

import com.smartreport.models.dto.*;
import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.service.IndicatorService;
import com.smartreport.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final CompanyRepository companyRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final IndicatorService indicatorService;

    private static final List<String> DEFAULT_METRICS = List.of("revenue", "profit", "grossMargin", "debtRatio", "cashFlow");
    private static final List<String> KPI_KEYS = List.of("revenue", "profit", "debtRatio", "cashFlow");

    @Override
    public KpiResponse getKpi(String companyCode) {
        Company company = companyRepository.findById(companyCode)
                .orElseThrow(() -> new NoSuchElementException("Company not found: " + companyCode));

        FinancialReport latest = reportRepository
                .findTopByCompanyCodeAndStatusOrderByReportYearDesc(companyCode, 1)
                .orElseThrow(() -> new NoSuchElementException("No report for: " + companyCode));

        FinancialReport previous = findPreviousReport(companyCode, latest.getReportYear());

        List<FinancialIndicator> indicators = indicatorRepository.findByReportId(latest.getId());
        List<FinancialIndicator> prevIndicators = previous != null
                ? indicatorRepository.findByReportId(previous.getId()) : List.of();

        Map<String, BigDecimal> prevMap = toValueMap(prevIndicators);

        List<KpiItem> kpis = new ArrayList<>();
        for (FinancialIndicator fi : indicators) {
            if (!KPI_KEYS.contains(fi.getIndicatorKey())) continue;
            BigDecimal yoy = indicatorService.calculateYoY(
                    fi.getValue(), prevMap.get(fi.getIndicatorKey()));
            String trend = indicatorService.determineTrend(fi.getIndicatorKey(), yoy);
            kpis.add(KpiItem.builder()
                    .key(fi.getIndicatorKey())
                    .name(indicatorService.getName(fi.getIndicatorKey()))
                    .value(fi.getValue())
                    .unit(indicatorService.getUnit(fi.getIndicatorKey()))
                    .yoy(yoy)
                    .trend(trend)
                    .build());
        }

        return KpiResponse.builder()
                .company(CompanyBrief.from(company))
                .reportYear(latest.getReportYear())
                .reportType(latest.getReportType())
                .kpis(kpis)
                .build();
    }

    @Override
    public TimelineResponse getTimeline(String companyCode, List<String> metricKeys) {
        if (metricKeys == null || metricKeys.isEmpty()) {
            metricKeys = DEFAULT_METRICS;
        }

        List<FinancialReport> reports = reportRepository
                .findByCompanyCodeAndReportYearBetweenOrderByReportYearAsc(companyCode, 2020, 2024);

        List<String> years = reports.stream()
                .map(r -> String.valueOf(r.getReportYear())).toList();

        List<Long> reportIds = reports.stream().map(FinancialReport::getId).toList();
        List<FinancialIndicator> allIndicators = indicatorRepository.findByReportIdIn(reportIds);

        Map<Long, Map<String, BigDecimal>> dataMap = new HashMap<>();
        for (FinancialIndicator fi : allIndicators) {
            dataMap.computeIfAbsent(fi.getReportId(), k -> new HashMap<>())
                    .put(fi.getIndicatorKey(), fi.getValue());
        }

        List<TimelineResponse.MetricSeries> series = new ArrayList<>();
        for (String key : metricKeys) {
            List<BigDecimal> values = new ArrayList<>();
            for (FinancialReport report : reports) {
                Map<String, BigDecimal> row = dataMap.getOrDefault(report.getId(), Map.of());
                values.add(row.get(key));
            }
            series.add(TimelineResponse.MetricSeries.builder()
                    .key(key).name(indicatorService.getName(key))
                    .unit(indicatorService.getUnit(key)).values(values).build());
        }

        return TimelineResponse.builder().years(years).metrics(series).build();
    }

    @Override
    public KpiResponse getLatestReport(String companyCode) {
        return getKpi(companyCode);
    }

    private FinancialReport findPreviousReport(String companyCode, int currentYear) {
        List<FinancialReport> reports = reportRepository
                .findByCompanyCodeOrderByReportYearDesc(companyCode);
        return reports.stream()
                .filter(r -> r.getReportYear() < currentYear)
                .findFirst().orElse(null);
    }

    private Map<String, BigDecimal> toValueMap(List<FinancialIndicator> indicators) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (FinancialIndicator fi : indicators) {
            map.put(fi.getIndicatorKey(), fi.getValue());
        }
        return map;
    }

    @Override
    public IndicatorDetailResponse getIndicators(String companyCode) {
        FinancialReport latest = reportRepository
                .findTopByCompanyCodeAndStatusOrderByReportYearDesc(companyCode, 1)
                .orElseThrow(() -> new NoSuchElementException("No report for: " + companyCode));

        FinancialReport previous = findPreviousReport(companyCode, latest.getReportYear());

        List<FinancialIndicator> indicators = indicatorRepository.findByReportId(latest.getId());
        List<FinancialIndicator> prevIndicators = previous != null
                ? indicatorRepository.findByReportId(previous.getId()) : List.of();

        Map<String, BigDecimal> prevMap = toValueMap(prevIndicators);

        List<IndicatorDetailResponse.IndicatorDetail> details = indicators.stream()
                .map(fi -> {
                    BigDecimal yoy = indicatorService.calculateYoY(
                            fi.getValue(), prevMap.get(fi.getIndicatorKey()));
                    String trend = indicatorService.determineTrend(fi.getIndicatorKey(), yoy);
                    return IndicatorDetailResponse.IndicatorDetail.builder()
                            .key(fi.getIndicatorKey())
                            .name(indicatorService.getName(fi.getIndicatorKey()))
                            .unit(indicatorService.getUnit(fi.getIndicatorKey()))
                            .value(fi.getValue())
                            .yoy(yoy)
                            .trend(trend)
                            .category(indicatorService.getCategory(fi.getIndicatorKey()))
                            .explanation(indicatorService.getExplanation(fi.getIndicatorKey()))
                            .evaluation(indicatorService.getEvaluation(fi.getIndicatorKey(), fi.getValue()))
                            .sortOrder(indicatorService.getSortOrder(fi.getIndicatorKey()))
                            .build();
                })
                .sorted(Comparator.comparingInt(IndicatorDetailResponse.IndicatorDetail::getSortOrder))
                .collect(Collectors.toList());

        return IndicatorDetailResponse.builder().indicators(details).build();
    }
}
