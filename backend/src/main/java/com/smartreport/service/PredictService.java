package com.smartreport.service;

import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.IndicatorDefinition;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.IndicatorDefinitionRepository;
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
    private final IndicatorDefinitionRepository definitionRepository;
    private final CompanyRepository companyRepository;
    private final IndicatorService indicatorService;

    private static final int HISTORY_YEARS = 5;
    private static final int PREDICT_YEARS = 2;
    private static final int PREDICT_METRIC_LIMIT = 3;
    private static final List<String> BANK_PRIORITY_KEYS = List.of("revenue", "profit", "debtRatio", "cashFlow", "totalAssets", "totalLiabilities", "netMargin", "roe");

    public Map<String, Object> predict(String companyCode) {
        List<FinancialReport> reports = reportRepository
                .findActiveByCompanyCodeOrderForDisplay(companyCode);

        // Take last 5 years
        List<FinancialReport> recent = reports.stream()
                .filter(report -> !indicatorRepository.findByReportId(report.getId()).isEmpty())
                .limit(HISTORY_YEARS)
                .sorted(Comparator.comparing(FinancialReport::getReportYear))
                .collect(Collectors.toList());

        if (recent.size() < 3) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("years", List.of());
            empty.put("series", List.of());
            empty.put("insights", Map.of());
            return empty;
        }

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

        List<String> metrics = selectPredictMetrics(companyCode, recent, byReport);
        for (String metric : metrics) {
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

        for (String metric : metrics) {
            List<BigDecimal> vals = data.get(metric);
            if (vals == null || vals.size() < 3 || vals.stream().anyMatch(Objects::isNull)) continue;

            int actualN = vals.size();  // Use actual data count, not fixed 5

            // Best-fit regression: tries exponential, weighted quadratic, quadratic
            double[] qr = bestFitRegression(vals);
            double a = qr[0];
            double b = qr[1];
            double c = qr[2];
            double r2 = qr[3];
            boolean isExponential = (Math.abs(a) < 1e-9 && Math.abs(b) > 1e-9);

            // Calculate historical YoY growth and RECENT trend for damping
            double avgGrowth = 0;
            int growthCount = 0;
            for (int i = 1; i < vals.size(); i++) {
                BigDecimal prev = vals.get(i - 1);
                BigDecimal curr = vals.get(i);
                if (prev != null && curr != null && prev.compareTo(BigDecimal.ZERO) != 0) {
                    avgGrowth += curr.subtract(prev).doubleValue() / prev.doubleValue();
                    growthCount++;
                }
            }
            double histGrowth = growthCount > 0 ? avgGrowth / growthCount : 0;

            // Detect recent trend: last 2 points, or collapsed-from-peak
            BigDecimal secondLast = vals.get(vals.size() - 2);
            BigDecimal lastActual = vals.get(vals.size() - 1);
            boolean recentDown = lastActual.compareTo(secondLast) < 0;
            // Also check if value collapsed from peak (>50% drop) → treat as declining
            if (!recentDown && vals.size() >= 3) {
                BigDecimal maxVal = vals.get(0);
                for (BigDecimal v : vals) { if (v.compareTo(maxVal) > 0) maxVal = v; }
                if (maxVal.compareTo(BigDecimal.ZERO) > 0 && lastActual.compareTo(maxVal) < 0) {
                    double dropRatio = lastActual.doubleValue() / maxVal.doubleValue();
                    if (dropRatio < 0.5) recentDown = true;
                }
            }

            // Actual values
            List<BigDecimal> actualVals = new ArrayList<>(vals);
            for (int i = 0; i < PREDICT_YEARS; i++) actualVals.add(null);

            // Predicted values: fitted for history, damped for future
            List<BigDecimal> predictedVals = new ArrayList<>();

            for (int i = 0; i < actualN; i++) {
                double x = i + 1;
                double fitted = isExponential ? Math.exp(b * x + c) : (a * x * x + b * x + c);
                predictedVals.add(BigDecimal.valueOf(fitted).setScale(2, RoundingMode.HALF_UP));
            }
            for (int i = 0; i < PREDICT_YEARS; i++) {
                double x = actualN + i + 1;
                double raw = isExponential ? Math.exp(b * x + c) : (a * x * x + b * x + c);
                double damped = dampedPredict(lastActual.doubleValue(), raw, i + 1, histGrowth, recentDown);
                predictedVals.add(BigDecimal.valueOf(damped).setScale(2, RoundingMode.HALF_UP));
            }

            // Actual series (solid)
            String name = indicatorService.getName(metric);
            String unit = indicatorService.getUnit(metric);
            series.add(buildSeries(metric, name, "solid", actualVals));
            // Predicted series (dashed)
            series.add(buildSeries(metric + "_pred", name + "（预测）", "dashed", predictedVals));

            // Insight
            BigDecimal lastVal = vals.get(vals.size() - 1);
            BigDecimal nextPred = predictedVals.get(actualN);
            BigDecimal change = nextPred.subtract(lastVal)
                    .divide(lastVal.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            // Determine trend: compare last prediction to last actual
            boolean isGrowth = nextPred.compareTo(lastVal) > 0;

            Map<String, Object> metricInsight = new LinkedHashMap<>();
            metricInsight.put("name", name);
            metricInsight.put("unit", unit);
            metricInsight.put("lastValue", lastVal);
            metricInsight.put("predictedValue", nextPred);
            metricInsight.put("change", change.setScale(1, RoundingMode.HALF_UP));
            metricInsight.put("trend", isGrowth ? "增长" : "下降");
            metricInsight.put("r2", BigDecimal.valueOf(r2).setScale(3, RoundingMode.HALF_UP));
            metricInsight.put("slope", BigDecimal.valueOf(b).setScale(2, RoundingMode.HALF_UP));
            insights.put(metric, metricInsight);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("years", yearLabels);
        result.put("series", series);
        result.put("insights", insights);
        return result;
    }

    private List<String> selectPredictMetrics(String companyCode,
                                              List<FinancialReport> reports,
                                              Map<Long, Map<String, BigDecimal>> byReport) {
        List<IndicatorDefinition> definitions = definitionRepository.findAllByOrderBySortOrderAsc();
        Map<String, Integer> industryPriority = buildIndustryPriority(companyCode);
        return definitions.stream()
                .sorted(Comparator.comparing(def -> industryPriority.getOrDefault(def.getKey(), 999)))
                .map(IndicatorDefinition::getKey)
                .filter(key -> hasCompleteHistory(reports, byReport, key))
                .limit(PREDICT_METRIC_LIMIT)
                .toList();
    }

    private boolean hasCompleteHistory(List<FinancialReport> reports,
                                       Map<Long, Map<String, BigDecimal>> byReport,
                                       String key) {
        for (FinancialReport report : reports) {
            Map<String, BigDecimal> row = byReport.getOrDefault(report.getId(), Map.of());
            if (row.get(key) == null) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Integer> buildIndustryPriority(String companyCode) {
        Map<String, Integer> priority = new HashMap<>();
        String industry = companyRepository.findById(companyCode).map(c -> c.getIndustry()).orElse("");
        if (industry.contains("银行")) {
            for (int i = 0; i < BANK_PRIORITY_KEYS.size(); i++) {
                priority.put(BANK_PRIORITY_KEYS.get(i), i);
            }
        }
        return priority;
    }

    /**
     * Simple linear regression: y = a*x + b
     * Returns [slope, intercept, rSquared]
     */
    /**
     * Quadratic regression: y = a*x² + b*x + c
     * Returns [a, b, c, rSquared]
     */
    /**
     * Best-fit regression: tries exponential + weighted quadratic, picks best R².
     * Returns {a, b, c, rSquared} where curve is a*x² + b*x + c
     * (for exponential model, coefficients are converted to quadratic form approximation)
     */
    private double[] bestFitRegression(List<BigDecimal> values) {
        int n = values.size();
        if (n < 3) return quadraticRegression(values);

        double[] quadratic = quadraticRegression(values);
        double[] weightedQuad = weightedQuadratic(values);
        double[] exponential = exponentialRegression(values);

        double bestR2 = quadratic[3];
        double[] best = quadratic;

        if (weightedQuad[3] > bestR2) { bestR2 = weightedQuad[3]; best = weightedQuad; }
        if (exponential[3] > bestR2) { bestR2 = exponential[3]; best = exponential; }

        return best;
    }

    /**
     * Weighted quadratic regression: recent years get exponentially higher weights.
     * Weight[i] = exp(lambda * (i - n + 1)) where lambda controls recency bias.
     */
    private double[] weightedQuadratic(List<BigDecimal> values) {
        int n = values.size();
        double lambda = 0.5; // recency weight factor
        double[] w = new double[n];
        double sumW = 0;
        for (int i = 0; i < n; i++) {
            w[i] = Math.exp(lambda * (i - n + 1));
            sumW += w[i];
        }
        for (int i = 0; i < n; i++) w[i] /= sumW;

        double sumWx = 0, sumWx2 = 0, sumWx3 = 0, sumWx4 = 0;
        double sumWy = 0, sumWxy = 0, sumWx2y = 0, sumWy2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            double wi = w[i];
            double x2 = x * x;
            sumWx += wi * x;
            sumWx2 += wi * x2;
            sumWx3 += wi * x2 * x;
            sumWx4 += wi * x2 * x2;
            sumWy += wi * y;
            sumWxy += wi * x * y;
            sumWx2y += wi * x2 * y;
            sumWy2 += wi * y * y;
        }

        double[][] mat = {
            {sumWx4, sumWx3, sumWx2, sumWx2y},
            {sumWx3, sumWx2, sumWx,  sumWxy},
            {sumWx2, sumWx,  1.0,   sumWy}
        };
        double[] coeffs = solveGauss(mat);
        double a = coeffs[0], b = coeffs[1], c = coeffs[2];

        double yMean = sumWy;
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            double pred = a * x * x + b * x + c;
            ssTot += w[i] * (y - yMean) * (y - yMean);
            ssRes += w[i] * (y - pred) * (y - pred);
        }
        double r2 = ssTot > 0 ? 1 - ssRes / ssTot : 0;

        if (Math.abs(a) < 1e-9 || r2 < 0.3) {
            double[] linear = linearRegression(values);
            return new double[]{0, linear[0], linear[1], Math.max(r2, linear[2])};
        }
        return new double[]{a, b, c, r2};
    }

    /**
     * Exponential regression: ln(y) = a*x + b  =>  y = exp(b) * exp(a*x)
     * Returns quadratic-form coefficients {a2, b2, c2, r2} for unified interface.
     * For prediction we use y = exp(c2 + b2*x + a2*x²), fitting via ln(y).
     */
    private double[] exponentialRegression(List<BigDecimal> values) {
        int n = values.size();
        double sumX = 0, sumX2 = 0, sumY = 0, sumXY = 0, sumY2 = 0;

        // Fit ln(y) = a*x + b
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            if (y <= 0) {
                // Can't take log of non-positive; fall back to quadratic
                return quadraticRegression(values);
            }
            double lnY = Math.log(y);
            sumX += x;
            sumX2 += x * x;
            sumY += lnY;
            sumXY += x * lnY;
            sumY2 += lnY * lnY;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // R² on log scale
        double yMean = sumY / n;
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double lnY = Math.log(values.get(i).doubleValue());
            double pred = slope * x + intercept;
            ssTot += (lnY - yMean) * (lnY - yMean);
            ssRes += (lnY - pred) * (lnY - pred);
        }
        double r2Log = ssTot > 0 ? 1 - ssRes / ssTot : 0;

        // Also compute R² on original scale
        double sumOrigY = 0;
        for (BigDecimal v : values) sumOrigY += v.doubleValue();
        double meanOrigY = sumOrigY / n;
        double ssTotOrig = 0, ssResOrig = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            double pred = Math.exp(slope * x + intercept);
            ssTotOrig += (y - meanOrigY) * (y - meanOrigY);
            ssResOrig += (y - pred) * (y - pred);
        }
        double r2Orig = ssTotOrig > 0 ? 1 - ssResOrig / ssTotOrig : 0;

        // Return as quadratic-like: we use 0 for a² term since exponential is ln-linear
        // For prediction, eval using exp formula directly via a=0 for quadratic
        return new double[]{0, slope, intercept, Math.max(r2Log, r2Orig)};
    }
    private double[] quadraticRegression(List<BigDecimal> values) {
        int n = values.size();
        double sumX = 0, sumX2 = 0, sumX3 = 0, sumX4 = 0;
        double sumY = 0, sumXY = 0, sumX2Y = 0, sumY2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            double x2 = x * x;
            sumX += x;
            sumX2 += x2;
            sumX3 += x2 * x;
            sumX4 += x2 * x2;
            sumY += y;
            sumXY += x * y;
            sumX2Y += x2 * y;
            sumY2 += y * y;
        }

        // Solve 3x3 system for a, b, c
        double[][] matrix = {
            {sumX4, sumX3, sumX2, sumX2Y},
            {sumX3, sumX2, sumX,  sumXY},
            {sumX2, sumX,  n,     sumY}
        };
        double[] coeffs = solveGauss(matrix);

        double a = coeffs[0];
        double b = coeffs[1];
        double c = coeffs[2];

        // R-squared
        double yMean = sumY / n;
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            double predicted = a * x * x + b * x + c;
            ssTot += (y - yMean) * (y - yMean);
            ssRes += (y - predicted) * (y - predicted);
        }
        double rSquared = ssTot > 0 ? 1 - ssRes / ssTot : 0;

        // If quadratic term is negligible or makes trend explode unreasonably,
        // fall back to linear
        if (Math.abs(a) < 1e-9 || rSquared < 0.5) {
            double[] linear = linearRegression(values);
            return new double[]{0, linear[0], linear[1], Math.max(rSquared, linear[2])};
        }

        return new double[]{a, b, c, rSquared};
    }

    /**
     * Gaussian elimination for 3x3 system
     */
    private double[] solveGauss(double[][] mat) {
        int n = 3;
        for (int col = 0; col < n; col++) {
            // Partial pivot
            int maxRow = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(mat[row][col]) > Math.abs(mat[maxRow][col])) {
                    maxRow = row;
                }
            }
            double[] tmp = mat[col];
            mat[col] = mat[maxRow];
            mat[maxRow] = tmp;

            for (int row = col + 1; row < n; row++) {
                double factor = mat[row][col] / mat[col][col];
                for (int j = col; j <= n; j++) {
                    mat[row][j] -= factor * mat[col][j];
                }
            }
        }
        double[] result = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0;
            for (int j = i + 1; j < n; j++) {
                sum += mat[i][j] * result[j];
            }
            result[i] = (mat[i][n] - sum) / mat[i][i];
        }
        return result;
    }

    /**
     * Apply growth damping: as we project further into the future,
     * the growth rate gradually decays toward a long-term sustainable rate.
     */
    private double dampedPredict(double baseVal, double rawPred, int yearAhead, double yoyGrowth, boolean recentDown) {
        double maxGrowth = 0.50;
        double minGrowth = -0.30;

        // If recent trend is down, prediction should NOT reverse upward
        if (recentDown && rawPred > baseVal * 1.05) {
            rawPred = baseVal * (1 + Math.min(yoyGrowth, 0));
        }

        // Decay factor: further predictions are more conservative
        double decay = Math.pow(0.7, yearAhead - 1);
        double dampedGrowth = yoyGrowth * decay;

        // Clamp to reasonable bounds
        dampedGrowth = Math.max(minGrowth, Math.min(maxGrowth, dampedGrowth));

        // Blend: 60% regression, 40% growth-rate-based
        double growthPred = baseVal * (1 + dampedGrowth);
        return rawPred * 0.6 + growthPred * 0.4;
    }
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
