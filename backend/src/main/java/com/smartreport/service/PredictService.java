package com.smartreport.service;

import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictService {

    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final IndicatorService indicatorService;

    private static final List<String> PREDICT_METRICS = List.of("revenue", "profit");
    private static final int HISTORY_YEARS = 5;
    private static final int PREDICT_YEARS = 2;

    public Map<String, Object> predict(String companyCode) {
        List<FinancialReport> reports = reportRepository
                .findByCompanyCodeOrderByReportYearDesc(companyCode);

        // Take last 5 years
        List<FinancialReport> recent = reports.stream()
                .sorted(Comparator.comparing(FinancialReport::getReportYear).reversed())
                .limit(HISTORY_YEARS)
                .sorted(Comparator.comparing(FinancialReport::getReportYear))
                .collect(Collectors.toList());

        List<Long> reportIds = recent.stream().map(FinancialReport::getId).toList();
        List<FinancialIndicator> allIndicators = indicatorRepository.findByReportIdIn(reportIds);

        // Build data map: key -> list of (year, value)
        Map<String, List<BigDecimal>> data = new LinkedHashMap<>();
        List<Integer> years = recent.stream().map(FinancialReport::getReportYear).toList();

        Map<Long, Map<String, BigDecimal>> byReport = new HashMap<>();
        for (FinancialIndicator fi : allIndicators) {
            byReport.computeIfAbsent(fi.getReportId(), k -> new HashMap<>())
                    .put(fi.getIndicatorKey(), fi.getValue());
        }

        for (String metric : PREDICT_METRICS) {
            List<BigDecimal> values = new ArrayList<>();
            for (FinancialReport r : recent) {
                Map<String, BigDecimal> row = byReport.getOrDefault(r.getId(), Map.of());
                values.add(row.get(metric));
            }
            data.put(metric, values);
        }

        // Linear regression for each metric
        List<Map<String, Object>> series = new ArrayList<>();
        List<String> allYears = new ArrayList<>();
        for (int y : years) allYears.add(String.valueOf(y));

        int lastYear = years.get(years.size() - 1);
        List<String> predictYears = new ArrayList<>();
        for (int i = 1; i <= PREDICT_YEARS; i++) {
            predictYears.add(String.valueOf(lastYear + i));
        }

        // Generate prediction years list
        List<String> yearLabels = new ArrayList<>(allYears);
        yearLabels.addAll(predictYears);

        Map<String, Object> insights = new LinkedHashMap<>();

        for (String metric : PREDICT_METRICS) {
            List<BigDecimal> vals = data.get(metric);
            if (vals == null || vals.size() < 3) continue;

            // Linear regression: fit y = a*x + b where x = year index (1-based)
            double[] lr = linearRegression(vals);
            double slope = lr[0];
            double intercept = lr[1];
            double r2 = lr[2];

            // Actual values
            List<BigDecimal> actualVals = new ArrayList<>(vals);
            // Pad with nulls for prediction years
            for (int i = 0; i < PREDICT_YEARS; i++) actualVals.add(null);

            // Predicted values: null for history, predicted for future
            List<BigDecimal> predictedVals = new ArrayList<>();
            for (int i = 0; i < HISTORY_YEARS; i++) {
                // Fitted values for history
                double fitted = slope * (i + 1) + intercept;
                predictedVals.add(BigDecimal.valueOf(fitted).setScale(2, RoundingMode.HALF_UP));
            }
            // Predict future
            for (int i = 0; i < PREDICT_YEARS; i++) {
                double p = slope * (HISTORY_YEARS + i + 1) + intercept;
                predictedVals.add(BigDecimal.valueOf(p).setScale(2, RoundingMode.HALF_UP));
            }

            // Upper/lower confidence (simple: ± 1 standard error)
            double stdErr = standardError(vals, slope, intercept);
            List<BigDecimal> upperVals = new ArrayList<>();
            List<BigDecimal> lowerVals = new ArrayList<>();
            for (int i = 0; i < HISTORY_YEARS + PREDICT_YEARS; i++) {
                Double mid = predictedVals.get(i).doubleValue();
                // Wider band for predictions
                double factor = (i < HISTORY_YEARS) ? 1.0 : 2.0;
                upperVals.add(BigDecimal.valueOf(mid + factor * stdErr * 2).setScale(2, RoundingMode.HALF_UP));
                lowerVals.add(BigDecimal.valueOf(Math.max(0, mid - factor * stdErr * 2)).setScale(2, RoundingMode.HALF_UP));
            }

            String name = indicatorService.getName(metric);
            String unit = indicatorService.getUnit(metric);

            // Actual series (solid)
            series.add(buildSeries(metric, name, "solid", actualVals));
            // Predicted series (dashed)
            series.add(buildSeries(metric + "_pred", name + "（预测）", "dashed", predictedVals));
            // Confidence bands
            series.add(buildSeries(metric + "_upper", name + "（上限）", "dashed", upperVals));
            series.add(buildSeries(metric + "_lower", name + "（下限）", "dashed", lowerVals));

            // Insight
            BigDecimal lastVal = vals.get(vals.size() - 1);
            BigDecimal nextPred = predictedVals.get(HISTORY_YEARS);
            BigDecimal change = nextPred.subtract(lastVal)
                    .divide(lastVal.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            Map<String, Object> metricInsight = new LinkedHashMap<>();
            metricInsight.put("name", name);
            metricInsight.put("unit", unit);
            metricInsight.put("lastValue", lastVal);
            metricInsight.put("predictedValue", nextPred);
            metricInsight.put("change", change.setScale(1, RoundingMode.HALF_UP));
            metricInsight.put("trend", slope > 0 ? "增长" : "下降");
            metricInsight.put("r2", BigDecimal.valueOf(r2).setScale(3, RoundingMode.HALF_UP));
            metricInsight.put("slope", BigDecimal.valueOf(slope).setScale(2, RoundingMode.HALF_UP));
            insights.put(metric, metricInsight);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("years", yearLabels);
        result.put("series", series);
        result.put("insights", insights);
        return result;
    }

    /**
     * Simple linear regression: y = a*x + b
     * Returns [slope, intercept, rSquared]
     */
    private double[] linearRegression(List<BigDecimal> values) {
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // R-squared
        double yMean = sumY / n;
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            double predicted = slope * x + intercept;
            ssTot += (y - yMean) * (y - yMean);
            ssRes += (y - predicted) * (y - predicted);
        }
        double rSquared = ssTot > 0 ? 1 - ssRes / ssTot : 0;

        return new double[]{slope, intercept, rSquared};
    }

    private double standardError(List<BigDecimal> actual, double slope, double intercept) {
        int n = actual.size();
        double sumSq = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * (i + 1) + intercept;
            double diff = actual.get(i).doubleValue() - predicted;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / Math.max(1, n - 2));
    }

    private Map<String, Object> buildSeries(String key, String name, String type, List<BigDecimal> values) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("key", key);
        s.put("name", name);
        s.put("type", type);
        s.put("values", values);
        return s;
    }
}
