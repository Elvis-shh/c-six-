package com.smartreport.service.impl;

import com.smartreport.models.dto.*;
import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.CompanyIndustryTagRepository;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.service.IndicatorService;
import com.smartreport.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final CompanyRepository companyRepository;
    private final CompanyIndustryTagRepository companyIndustryTagRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final IndicatorService indicatorService;

    private static final int KPI_LIMIT = 4;
    private static final int TIMELINE_LIMIT = 5;
    private static final List<String> FALLBACK_CORE_KEYS = List.of("revenue", "profit", "cashFlow", "netMargin", "debtRatio", "roe", "grossMargin", "rdExpenseRatio", "eps");

    @Override
    public KpiResponse getKpi(String companyCode) {
        Company company = companyRepository.findById(companyCode)
                .orElseThrow(() -> new NoSuchElementException("Company not found: " + companyCode));

        FinancialReport latest = findBestReport(companyCode)
                .orElseThrow(() -> new NoSuchElementException("No report for: " + companyCode));

        FinancialReport previous = findPreviousReport(companyCode, latest.getReportYear());

        List<FinancialIndicator> indicators = indicatorRepository.findByReportId(latest.getId());
        List<FinancialIndicator> prevIndicators = previous != null
                ? indicatorRepository.findByReportId(previous.getId()) : List.of();

        Map<String, BigDecimal> currentMap = toValueMap(indicators);
        Map<String, BigDecimal> prevMap = toValueMap(prevIndicators);
        List<String> selectedKeys = selectCoreKeys(company, List.of(latest), Map.of(latest.getId(), currentMap), KPI_LIMIT);

        List<KpiItem> kpis = new ArrayList<>();
        for (String key : selectedKeys) {
            BigDecimal value = currentMap.get(key);
            if (value == null) {
                continue;
            }
            BigDecimal yoy = indicatorService.calculateYoY(
                    value, prevMap.get(key));
            String trend = indicatorService.determineTrend(key, yoy);
            kpis.add(KpiItem.builder()
                    .key(key)
                    .name(indicatorService.getName(key))
                    .value(value)
                    .unit(indicatorService.getUnit(key))
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
        List<FinancialReport> reports = findRecentReportsWithIndicators(companyCode, TIMELINE_LIMIT);
        if (reports.isEmpty()) {
            return TimelineResponse.builder().years(List.of()).metrics(List.of()).build();
        }

        List<String> years = reports.stream()
                .map(r -> String.valueOf(r.getReportYear())).toList();

        List<Long> reportIds = reports.stream().map(FinancialReport::getId).toList();
        List<FinancialIndicator> allIndicators = indicatorRepository.findByReportIdIn(reportIds);

        Map<Long, Map<String, BigDecimal>> dataMap = new HashMap<>();
        for (FinancialIndicator fi : allIndicators) {
            dataMap.computeIfAbsent(fi.getReportId(), k -> new HashMap<>())
                    .put(fi.getIndicatorKey(), fi.getValue());
        }

        if (metricKeys == null || metricKeys.isEmpty()) {
            Company company = companyRepository.findById(companyCode).orElse(null);
            metricKeys = selectCoreKeys(company, reports, dataMap, TIMELINE_LIMIT);
        } else {
            List<String> requested = metricKeys.stream()
                    .filter(key -> hasMetric(dataMap, reports, key))
                    .toList();
            Company company = companyRepository.findById(companyCode).orElse(null);
            metricKeys = requested.isEmpty() ? selectCoreKeys(company, reports, dataMap, TIMELINE_LIMIT) : requested;
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
        List<FinancialReport> reports = findReportsWithIndicators(companyCode);
        return reports.stream()
                .filter(r -> r.getReportYear() < currentYear)
                .findFirst().orElse(null);
    }

    private Optional<FinancialReport> findBestReport(String companyCode) {
        return findReportsWithIndicators(companyCode).stream().findFirst();
    }

    private Map<String, BigDecimal> toValueMap(List<FinancialIndicator> indicators) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (FinancialIndicator fi : indicators) {
            map.put(fi.getIndicatorKey(), fi.getValue());
        }
        return map;
    }

    private List<FinancialReport> findReportsWithIndicators(String companyCode) {
        return reportRepository.findByCompanyCodeOrderForDisplayIncludingParsed(companyCode).stream()
                .filter(report -> !indicatorRepository.findByReportId(report.getId()).isEmpty())
                .toList();
    }

    private List<FinancialReport> findRecentReportsWithIndicators(String companyCode, int limit) {
        List<FinancialReport> reports = findReportsWithIndicators(companyCode).stream()
                .limit(limit)
                .toList();
        List<FinancialReport> ascending = new ArrayList<>(reports);
        ascending.sort(Comparator.comparing(FinancialReport::getReportYear));
        return ascending;
    }

    private List<String> selectCoreKeys(Company company,
                                        List<FinancialReport> reports,
                                        Map<Long, Map<String, BigDecimal>> dataMap,
                                        int limit) {
        List<String> candidateKeys = coreKeyCandidates(company);
        List<String> selected = candidateKeys.stream()
                .filter(key -> hasMetric(dataMap, reports, key))
                .limit(limit)
                .collect(Collectors.toCollection(ArrayList::new));
        boolean hasAny = !selected.isEmpty();
        if (!hasAny && !reports.isEmpty()) {
            Long latestReportId = reports.get(reports.size() - 1).getId();
            Map<String, BigDecimal> latestRow = dataMap.getOrDefault(latestReportId, Map.of());
            latestRow.keySet().stream()
                    .filter(key -> latestRow.get(key) != null)
                    .findFirst()
                    .ifPresent(selected::add);
        }
        return selected.stream().distinct().toList();
    }

    private List<String> coreKeyCandidates(Company company) {
        List<String> covered = new ArrayList<>();
        if (company != null && companyIndustryTagRepository.existsByCompanyCodeAndTag(company.getCode(), "中证A50")) {
            covered.addAll(indicatorRepository.findMostCoveredKeysByIndustryAndSource(
                    "中证A50", "crawler", PageRequest.of(0, 12)));
        }
        for (String key : FALLBACK_CORE_KEYS) {
            if (!covered.contains(key)) {
                covered.add(key);
            }
        }
        return covered;
    }

    private boolean hasMetric(Map<Long, Map<String, BigDecimal>> dataMap,
                              List<FinancialReport> reports,
                              String key) {
        return reports.stream()
                .map(FinancialReport::getId)
                .map(dataMap::get)
                .filter(Objects::nonNull)
                .map(row -> row.get(key))
                .anyMatch(Objects::nonNull);
    }

    @Override
    public IndicatorDetailResponse getIndicators(String companyCode) {
        FinancialReport latest = findBestReport(companyCode)
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
