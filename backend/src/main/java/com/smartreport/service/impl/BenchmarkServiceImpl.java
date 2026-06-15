package com.smartreport.service.impl;

import com.smartreport.models.dto.BenchmarkResponse;
import com.smartreport.models.dto.BenchmarkResponse.IndicatorBenchmark;
import com.smartreport.models.entity.*;
import com.smartreport.repository.*;
import com.smartreport.service.BenchmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkServiceImpl implements BenchmarkService {

    private final CompanyRepository companyRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final IndustryAverageRepository industryAverageRepository;

    private static final List<String> BENCHMARK_KEYS = List.of("grossMargin", "netMargin", "debtRatio", "roe");

    @Override
    public BenchmarkResponse getBenchmark(String companyCode, int year) {
        Company company = companyRepository.findById(companyCode)
                .orElseThrow(() -> new NoSuchElementException("Company not found"));

        List<IndustryAverage> averages = industryAverageRepository
                .findByIndustryAndIndicatorKeyIn(company.getIndustry(), BENCHMARK_KEYS);

        FinancialReport report = reportRepository
                .findTopByCompanyCodeAndStatusOrderByReportYearDesc(companyCode, 1)
                .orElse(null);

        Map<String, BigDecimal> companyValues = new HashMap<>();
        if (report != null) {
            indicatorRepository.findByReportId(report.getId())
                    .forEach(fi -> companyValues.put(fi.getIndicatorKey(), fi.getValue()));
        }

        Map<String, IndustryAverage> avgMap = new HashMap<>();
        for (IndustryAverage avg : averages) {
            avgMap.put(avg.getIndicatorKey(), avg);
        }

        List<IndicatorBenchmark> indicators = new ArrayList<>();
        for (String key : BENCHMARK_KEYS) {
            IndustryAverage avg = avgMap.get(key);
            if (avg == null) continue;

            BigDecimal companyVal = companyValues.getOrDefault(key, BigDecimal.ZERO);

            String rank = computeRank(companyVal, avg.getAvgValue());
            String rankLabel = switch (rank) {
                case "top5%" -> "行业前 5%";
                case "top10%" -> "行业前 10%";
                case "top25%" -> "行业前 25%";
                case "bottom50%" -> "行业后 50%";
                default -> "行业平均";
            };

            indicators.add(IndicatorBenchmark.builder()
                    .key(key).name(getIndicatorName(key))
                    .companyValue(companyVal)
                    .industryAvg(avg.getAvgValue())
                    .industryMedian(avg.getMedianValue())
                    .rank(rank).rankLabel(rankLabel)
                    .unit(key.contains("Margin") || key.equals("roe") || key.equals("debtRatio") ? "%" : "亿")
                    .build());
        }

        return BenchmarkResponse.builder()
                .industry(company.getIndustry())
                .indicators(indicators)
                .build();
    }

    private String computeRank(BigDecimal companyValue, BigDecimal avgValue) {
        if (companyValue == null || avgValue == null
                || avgValue.compareTo(BigDecimal.ZERO) == 0) return "unknown";
        BigDecimal ratio = companyValue.divide(avgValue, 2, java.math.RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("1.5")) > 0) return "top5%";
        if (ratio.compareTo(new BigDecimal("1.2")) > 0) return "top10%";
        if (ratio.compareTo(new BigDecimal("1.0")) > 0) return "top25%";
        return "bottom50%";
    }

    private String getIndicatorName(String key) {
        return switch (key) {
            case "grossMargin" -> "毛利率";
            case "netMargin" -> "净利率";
            case "debtRatio" -> "资产负债率";
            case "roe" -> "净资产收益率(ROE)";
            default -> key;
        };
    }
}
