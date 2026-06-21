package com.smartreport.service;

import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 预测洞察文案生成服务
 * 基于指标实际数据生成 4 段自然语言解读文案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictInsightService {

    private final CompanyRepository companyRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final PredictService predictService;
    private final IndicatorService indicatorService;

    private static final int HISTORY_YEARS = 5;

    /**
     * 生成完整的预测洞察（4 段文案 + 免责声明）
     */
    public Map<String, Object> generateInsights(String companyCode) {
        Map<String, Object> result = new LinkedHashMap<>();

        Company company = companyRepository.findById(companyCode).orElse(null);
        if (company == null) {
            result.put("error", "公司不存在");
            return result;
        }

        Map<String, Object> predictData = predictService.predict(companyCode);
        @SuppressWarnings("unchecked")
        Map<String, Object> insights = (Map<String, Object>) predictData.getOrDefault("insights", Map.of());

        if (insights.isEmpty()) {
            result.put("message", "历史数据不足，无法生成预测洞察");
            return result;
        }

        String companyName = company.getName();
        String industry = company.getIndustry() != null ? company.getIndustry() : "未知行业";

        // 获取历史数据用于更丰富的文案
        List<FinancialReport> reports = reportRepository
                .findActiveByCompanyCodeOrderForDisplay(companyCode);
        List<FinancialReport> recent = reports.stream()
                .filter(r -> !indicatorRepository.findByReportId(r.getId()).isEmpty())
                .sorted(Comparator.comparing(FinancialReport::getReportYear))
                .collect(java.util.stream.Collectors.toList());

        // 1. 营收趋势描述
        Map<String, Object> revenueTrend = buildRevenueTrend(insights, companyName, recent);
        result.put("revenueTrend", revenueTrend);

        // 2. 盈利能力预测
        Map<String, Object> profitOutlook = buildProfitOutlook(insights, companyName);
        result.put("profitOutlook", profitOutlook);

        // 3. 关键假设
        List<String> assumptions = buildAssumptions(companyName, industry);
        result.put("assumptions", assumptions);

        // 4. 风险提示
        String riskNote = buildRiskNote(industry);
        result.put("riskNote", riskNote);

        // 公司名称
        result.put("companyName", companyName);

        return result;
    }

    private Map<String, Object> buildRevenueTrend(Map<String, Object> insights, String companyName,
                                                   List<FinancialReport> recent) {
        Map<String, Object> trend = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        Map<String, Object> revenueInsight = (Map<String, Object>) insights.get("revenue");

        if (revenueInsight != null) {
            BigDecimal predictedValue = (BigDecimal) revenueInsight.get("predictedValue");
            BigDecimal change = (BigDecimal) revenueInsight.get("change");
            String trendDir = (String) revenueInsight.get("trend");
            BigDecimal r2 = (BigDecimal) revenueInsight.get("r2");

            String unit = indicatorService.getUnit("revenue");
            String unitLabel = "亿".equals(unit) ? "亿" : unit;

            // 计算 CAGR
            String cagrText = "";
            if (recent.size() >= 3) {
                List<Long> reportIds = recent.stream().map(FinancialReport::getId).toList();
                List<FinancialIndicator> allIndicators = indicatorRepository.findByReportIdIn(reportIds);
                List<BigDecimal> revenueValues = new ArrayList<>();
                for (FinancialReport r : recent) {
                    for (FinancialIndicator fi : allIndicators) {
                        if (fi.getReportId().equals(r.getId()) && "revenue".equals(fi.getIndicatorKey())) {
                            revenueValues.add(fi.getValue());
                            break;
                        }
                    }
                }
                if (revenueValues.size() >= 2 && !revenueValues.stream().anyMatch(Objects::isNull)) {
                    BigDecimal cagr = indicatorService.calculateCAGR(revenueValues, revenueValues.size() - 1);
                    if (cagr != null) {
                        cagrText = String.format("近%d年复合增长率约%.1f%%，", revenueValues.size() - 1, cagr);
                    }
                }
            }

            String description;
            if (predictedValue != null && change != null) {
                description = String.format(
                        "%s%s预计下一年营收将达到 %.2f %s，同比%s %.1f%%。模型拟合度 R²=%.3f。",
                        cagrText, companyName, predictedValue, unitLabel, trendDir, change.abs(), r2
                );
            } else {
                description = String.format("%s营收数据可用年份不足，暂无法进行可靠预测。", companyName);
            }

            trend.put("title", "📈 营收趋势");
            trend.put("description", description);
        } else {
            trend.put("title", "📈 营收趋势");
            trend.put("description", "营收指标历史数据不足，暂无法生成趋势洞察。");
        }

        return trend;
    }

    private Map<String, Object> buildProfitOutlook(Map<String, Object> insights, String companyName) {
        Map<String, Object> outlook = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        Map<String, Object> profitInsight = (Map<String, Object>) insights.get("profit");

        if (profitInsight != null) {
            BigDecimal predictedValue = (BigDecimal) profitInsight.get("predictedValue");
            BigDecimal change = (BigDecimal) profitInsight.get("change");
            String trendDir = (String) profitInsight.get("trend");
            String unit = indicatorService.getUnit("profit");
            String unitLabel = "亿".equals(unit) ? "亿" : unit;

            String description;
            if (predictedValue != null && change != null) {
                description = String.format(
                        "归母净利润预计保持%.1f%%左右的%s趋势，下一年有望达到 %.2f %s。",
                        change.abs(), trendDir, predictedValue, unitLabel
                );
            } else {
                description = String.format("%s净利润数据不足以支撑可靠预测。", companyName);
            }

            outlook.put("title", "💰 盈利能力预测");
            outlook.put("description", description);
        } else {
            outlook.put("title", "💰 盈利能力预测");
            outlook.put("description", "净利润指标历史数据不足，暂无法生成盈利预测。");
        }

        return outlook;
    }

    private List<String> buildAssumptions(String companyName, String industry) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add("行业政策环境保持稳定");
        assumptions.add(companyName + "市场份额不发生重大变化");
        assumptions.add("宏观经济不出现严重衰退");

        if (industry.contains("白酒") || industry.contains("食品")) {
            assumptions.add("消费升级趋势延续，高端产品需求稳定");
        } else if (industry.contains("银行") || industry.contains("金融")) {
            assumptions.add("利率政策不发生大幅调整，资产质量保持稳定");
        } else if (industry.contains("新能源") || industry.contains("科技")) {
            assumptions.add("技术路线不发生颠覆性变化，行业竞争格局稳定");
        } else if (industry.contains("医药")) {
            assumptions.add("集采政策不发生超预期变化，研发管线进展顺利");
        }

        return assumptions;
    }

    private String buildRiskNote(String industry) {
        String base = "以上预测基于历史数据的趋势外推模型，未考虑突发事件、政策突变、市场剧烈波动等因素的影响。"
                + "实际业绩可能因市场环境、行业竞争、政策监管、管理层决策等多种因素而与预测值存在较大偏差。";
        return base;
    }
}
