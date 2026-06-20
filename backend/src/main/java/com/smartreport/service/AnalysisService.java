package com.smartreport.service;

import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.models.entity.HighlightRule;
import com.smartreport.models.entity.IndustryAverage;
import com.smartreport.models.entity.RiskRule;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.repository.HighlightRuleRepository;
import com.smartreport.repository.IndustryAverageRepository;
import com.smartreport.repository.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final CompanyRepository companyRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final HighlightRuleRepository highlightRuleRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final IndustryAverageRepository industryAverageRepository;

    public List<Map<String, Object>> getHighlights(String companyCode) {
        return evaluateRules(companyCode, true);
    }

    public List<Map<String, Object>> getRisks(String companyCode) {
        return evaluateRules(companyCode, false);
    }

    private List<Map<String, Object>> evaluateRules(String companyCode, boolean isHighlight) {
        Company company = companyRepository.findById(companyCode)
                .orElseThrow(() -> new NoSuchElementException("Company not found: " + companyCode));

        FinancialReport latest = reportRepository
                .findTopByCompanyCodeAndStatusOrderByReportYearDesc(companyCode, 1)
                .orElseThrow(() -> new NoSuchElementException("No report for: " + companyCode));

        FinancialReport prev = reportRepository
                .findByCompanyCodeOrderByReportYearDesc(companyCode).stream()
                .filter(r -> r.getReportYear() < latest.getReportYear())
                .findFirst().orElse(null);

        List<FinancialIndicator> indicators = indicatorRepository.findByReportId(latest.getId());
        List<FinancialIndicator> prevIndicators = prev != null
                ? indicatorRepository.findByReportId(prev.getId()) : List.of();

        // Build value map
        Map<String, BigDecimal> valMap = new HashMap<>();
        Map<String, BigDecimal> prevValMap = new HashMap<>();
        for (FinancialIndicator fi : indicators) {
            valMap.put(fi.getIndicatorKey(), fi.getValue());
        }
        for (FinancialIndicator fi : prevIndicators) {
            prevValMap.put(fi.getIndicatorKey(), fi.getValue());
        }

        // Get 2-year-ago data for prev_yoy calculations
        Map<String, BigDecimal> prev2ValMap = new HashMap<>();
        if (prev != null) {
            FinancialReport prev2 = reportRepository
                    .findByCompanyCodeOrderByReportYearDesc(companyCode).stream()
                    .filter(r -> r.getReportYear() < prev.getReportYear())
                    .findFirst().orElse(null);
            if (prev2 != null) {
                List<FinancialIndicator> prev2Indicators = indicatorRepository.findByReportId(prev2.getId());
                for (FinancialIndicator fi : prev2Indicators) {
                    prev2ValMap.put(fi.getIndicatorKey(), fi.getValue());
                }
            }
        }

        // Add derived values and industry averages
        Map<String, Object> context = buildContext(company, valMap, prevValMap, prev2ValMap, latest.getReportYear());

        // Evaluate rules
        List<?> rules = isHighlight
                ? highlightRuleRepository.findByEnabledOrderByPriorityAsc(1)
                : riskRuleRepository.findByEnabledOrderByPriorityAsc(1);

        List<Map<String, Object>> results = new ArrayList<>();
        int id = 1;
        for (Object ruleObj : rules) {
            String ruleKey, indicatorKey, condition, title, descTemplate, icon;
            if (isHighlight) {
                HighlightRule r = (HighlightRule) ruleObj;
                ruleKey = r.getRuleKey();
                indicatorKey = r.getIndicatorKey();
                condition = r.getConditionExpr();
                title = r.getTitle();
                descTemplate = r.getDescTemplate();
                icon = r.getIcon();
            } else {
                RiskRule r = (RiskRule) ruleObj;
                ruleKey = r.getRuleKey();
                indicatorKey = r.getIndicatorKey();
                condition = r.getConditionExpr();
                title = r.getTitle();
                descTemplate = r.getDescTemplate();
                icon = r.getIcon();
            }

            if (evaluateCondition(condition, context)) {
                String description = formatTemplate(descTemplate, context);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", id++);
                item.put("icon", icon);
                item.put("title", title);
                item.put("description", description);
                item.put("ruleKey", ruleKey);
                results.add(item);
            }
        }
        if (results.isEmpty()) {
            return buildFallbackInsights(valMap, prevValMap, isHighlight);
        }
        return results;
    }

    private List<Map<String, Object>> buildFallbackInsights(Map<String, BigDecimal> current,
                                                            Map<String, BigDecimal> previous,
                                                            boolean isHighlight) {
        List<Map<String, Object>> items = new ArrayList<>();
        int id = 1;

        if (current.containsKey("revenue")) {
            BigDecimal yoy = calculateYoY(current.get("revenue"), previous.get("revenue"));
            if (yoy != null && yoy.compareTo(BigDecimal.ZERO) > 0 == isHighlight) {
                items.add(insight(id++, isHighlight ? "📈" : "⚠️",
                        isHighlight ? "收入表现有支撑" : "收入增长承压",
                        buildMetricDescription("营业总收入", current.get("revenue"), "亿", yoy)));
            }
        }
        if (current.containsKey("profit")) {
            BigDecimal yoy = calculateYoY(current.get("profit"), previous.get("profit"));
            if (yoy != null && yoy.compareTo(BigDecimal.ZERO) > 0 == isHighlight) {
                items.add(insight(id++, isHighlight ? "💹" : "⚠️",
                        isHighlight ? "利润端延续改善" : "利润端出现压力",
                        buildMetricDescription("归母净利润", current.get("profit"), "亿", yoy)));
            }
        }
        if (current.containsKey("cashFlow")) {
            BigDecimal yoy = calculateYoY(current.get("cashFlow"), previous.get("cashFlow"));
            if (yoy != null && yoy.compareTo(BigDecimal.ZERO) > 0 == isHighlight) {
                items.add(insight(id++, isHighlight ? "💵" : "⚠️",
                        isHighlight ? "现金回笼情况较好" : "现金流波动需关注",
                        buildMetricDescription("经营现金流", current.get("cashFlow"), "亿", yoy)));
            }
        }
        if (current.containsKey("debtRatio")) {
            BigDecimal value = current.get("debtRatio");
            if (value != null) {
                boolean good = value.compareTo(BigDecimal.valueOf(60)) < 0;
                if (good == isHighlight) {
                    items.add(insight(id++, isHighlight ? "⚖️" : "⚠️",
                            isHighlight ? "资产负债结构较稳" : "资产负债率偏高",
                            "当前资产负债率为 " + value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "% 。"));
                }
            }
        }
        if (items.isEmpty()) {
            items.add(insight(id,
                    isHighlight ? "📌" : "🔎",
                    isHighlight ? "财报披露了若干关键经营指标" : "当前财报可提取风险线索有限",
                    isHighlight
                            ? "系统已根据当前财报提取出核心财务指标，可结合趋势图与指标详解继续研判。"
                            : "系统暂未命中明确规则，建议重点关注利润、现金流和负债相关指标变化。"));
        }
        return items;
    }

    private BigDecimal calculateYoY(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private String buildMetricDescription(String name, BigDecimal value, String unit, BigDecimal yoy) {
        return name + "为 " + value.setScale(2, RoundingMode.HALF_UP).toPlainString() + unit
                + "，同比 " + (yoy.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                + yoy.setScale(1, RoundingMode.HALF_UP).toPlainString() + "% 。";
    }

    private Map<String, Object> insight(int id, String icon, String title, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("icon", icon);
        item.put("title", title);
        item.put("description", description);
        item.put("ruleKey", "fallback");
        return item;
    }

    private Map<String, Object> buildContext(Company company,
                                              Map<String, BigDecimal> current,
                                              Map<String, BigDecimal> previous,
                                              Map<String, BigDecimal> prev2,
                                              int year) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("company", company.getName());
        ctx.put("industry", company.getIndustry());

        // Current values
        for (Map.Entry<String, BigDecimal> e : current.entrySet()) {
            ctx.put(e.getKey(), e.getValue());
        }
        // Previous values
        for (Map.Entry<String, BigDecimal> e : previous.entrySet()) {
            ctx.put(e.getKey() + "_prev", e.getValue());
        }

        // YoY calculations
        for (String key : current.keySet()) {
            BigDecimal cur = current.get(key);
            BigDecimal prv = previous.get(key);
            if (cur != null && prv != null && prv.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal yoy = cur.subtract(prv)
                        .divide(prv.abs(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                ctx.put(key + "_yoy", yoy);
                ctx.put("abs_" + key + "_yoy", yoy.abs());
            }
        }

        // Previous year YoY (e.g. profit_yoy_prev)
        for (String key : previous.keySet()) {
            BigDecimal prv = previous.get(key);
            BigDecimal prv2 = prev2.get(key);
            if (prv != null && prv2 != null && prv2.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal yoyPrev = prv.subtract(prv2)
                        .divide(prv2.abs(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                ctx.put(key + "_yoy_prev", yoyPrev);
            }
        }

        // Template ratios: e.g. cashFlow/profit ratio
        if (current.containsKey("cashFlow") && current.containsKey("profit")
                && current.get("profit").compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal ratio = current.get("cashFlow")
                    .divide(current.get("profit"), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            ctx.put("ratio", ratio);
        }

        // Industry averages
        List<IndustryAverage> avgs = industryAverageRepository
                .findByIndustryAndYear(company.getIndustry(), year);
        for (IndustryAverage ia : avgs) {
            ctx.put("industryAvg" + capitalize(ia.getIndicatorKey()),
                    ia.getAvgValue() != null ? ia.getAvgValue() : BigDecimal.ZERO);
        }

        return ctx;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private boolean evaluateCondition(String expr, Map<String, Object> ctx) {
        if (expr == null || expr.isBlank()) return true;
        try {
            // Handle IN condition: industry IN (白酒,房地产,教培)
            Matcher inMatcher = Pattern.compile("(\\w+)\\s+IN\\s+\\((.+)\\)", Pattern.CASE_INSENSITIVE).matcher(expr);
            if (inMatcher.find()) {
                String varName = inMatcher.group(1);
                String listStr = inMatcher.group(2);
                String[] items = listStr.split(",");
                Object val = ctx.get(varName);
                if (val == null) return false;
                for (String item : items) {
                    if (item.trim().equals(val.toString().trim())) return true;
                }
                return false;
            }

            // Handle arithmetic comparison FIRST: key OP var * number
            Matcher arithMatcher = Pattern.compile(
                    "([\\w_]+)\\s*(>=|<=|!=|>|<)\\s*([\\w_]+)\\s*\\*\\s*([\\d.]+)", Pattern.CASE_INSENSITIVE).matcher(expr);
            if (arithMatcher.find()) {
                String left = arithMatcher.group(1);
                String op = arithMatcher.group(2);
                String varRight = arithMatcher.group(3);
                double multiplier = Double.parseDouble(arithMatcher.group(4));

                Double leftVal = resolveValue(left, ctx);
                Double rightVal = resolveValue(varRight, ctx);
                if (leftVal == null || rightVal == null) return false;
                double threshold = rightVal * multiplier;

                return switch (op) {
                    case ">" -> leftVal > threshold;
                    case "<" -> leftVal < threshold;
                    case ">=" -> leftVal >= threshold;
                    case "<=" -> leftVal <= threshold;
                    case "!=" -> Math.abs(leftVal - threshold) > 0.0001;
                    default -> false;
                };
            }

            // Handle plain comparison: key OP value
            Matcher cmpMatcher = Pattern.compile(
                    "([\\w_]+)\\s*(>=|<=|!=|>|<)\\s*([\\w._\\-]+)", Pattern.CASE_INSENSITIVE).matcher(expr);
            if (cmpMatcher.find()) {
                String left = cmpMatcher.group(1);
                String op = cmpMatcher.group(2);
                String right = cmpMatcher.group(3);

                Double leftVal = resolveValue(left, ctx);
                Double rightVal = resolveValue(right, ctx);

                if (leftVal == null || rightVal == null) return false;
                return switch (op) {
                    case ">" -> leftVal > rightVal;
                    case "<" -> leftVal < rightVal;
                    case ">=" -> leftVal >= rightVal;
                    case "<=" -> leftVal <= rightVal;
                    case "!=" -> Math.abs(leftVal - rightVal) > 0.0001;
                    default -> false;
                };
            }

        } catch (Exception e) {
            log.warn("Failed to evaluate condition: {}", expr, e);
        }
        return false;
    }

    private Double resolveValue(String expr, Map<String, Object> ctx) {
        if (expr == null) return null;
        // Try to parse as number
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException ignored) {}

        // Look up in context
        Object val = ctx.get(expr);
        if (val instanceof BigDecimal bd) return bd.doubleValue();
        if (val instanceof Number num) return num.doubleValue();
        if (val instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String formatTemplate(String template, Map<String, Object> ctx) {
        if (template == null) return "";
        Matcher m = Pattern.compile("\\{(\\w+)\\}").matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            Object val = ctx.get(key);
            String replacement = formatContextValue(key, val);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String formatContextValue(String key, Object val) {
        if (val == null) return "—";
        if (val instanceof BigDecimal bd) {
            if (key.endsWith("_yoy") || key.endsWith("_yoy_prev") || key.startsWith("abs_")) {
                return bd.setScale(1, RoundingMode.HALF_UP).toPlainString();
            }
            if (key.contains("Margin") || key.contains("margin") || key.equals("roe")
                    || key.contains("DebtRatio") || key.contains("debtRatio")) {
                return bd.setScale(1, RoundingMode.HALF_UP).toPlainString();
            }
            return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (val instanceof Double d) {
            if (key.endsWith("_yoy") || key.endsWith("_yoy_prev")) {
                return String.format("%.1f", d);
            }
            return String.format("%.2f", d);
        }
        return val.toString();
    }
}
