package com.smartreport.service.impl;

import com.smartreport.models.entity.IndicatorDefinition;
import com.smartreport.repository.IndicatorDefinitionRepository;
import com.smartreport.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorServiceImpl implements IndicatorService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private final IndicatorDefinitionRepository definitionRepository;

    private IndicatorDefinition getDef(String key) {
        return definitionRepository.findById(key).orElse(null);
    }

    @Override
    public BigDecimal calculateYoY(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null
                || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous.abs(), MC)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateQoQ(BigDecimal current, BigDecimal previous) {
        return calculateYoY(current, previous);  // 公式相同
    }

    @Override
    public BigDecimal calculateCAGR(List<BigDecimal> values, int years) {
        if (values == null || values.size() < 2 || years <= 0) {
            return null;
        }
        BigDecimal start = values.get(0);
        BigDecimal end = values.get(values.size() - 1);
        if (start == null || end == null
                || start.compareTo(BigDecimal.ZERO) <= 0
                || end.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        double ratio = end.divide(start, MC).doubleValue();
        double cagr = Math.pow(ratio, 1.0 / years) - 1;
        return BigDecimal.valueOf(cagr * 100).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null
                || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, MC)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String determineTrend(String indicatorKey, BigDecimal yoy) {
        if (yoy == null) return "neutral";
        boolean isGoodWhenDown = "debtRatio".equals(indicatorKey);
        if (yoy.compareTo(BigDecimal.ZERO) > 0) {
            return isGoodWhenDown ? "down" : "up";
        } else if (yoy.compareTo(BigDecimal.ZERO) < 0) {
            return isGoodWhenDown ? "down_good" : "down";
        }
        return "neutral";
    }

    @Override
    public String getUnit(String indicatorKey) {
        IndicatorDefinition def = getDef(indicatorKey);
        return def != null ? def.getUnit() : "";
    }

    @Override
    public String getName(String indicatorKey) {
        IndicatorDefinition def = getDef(indicatorKey);
        return def != null ? def.getName() : indicatorKey;
    }

    @Override
    public Map<String, Map<String, String>> getAllDefinitions() {
        List<IndicatorDefinition> all = definitionRepository.findAll();
        Map<String, Map<String, String>> result = new java.util.LinkedHashMap<>();
        for (IndicatorDefinition def : all) {
            Map<String, String> item = new java.util.LinkedHashMap<>();
            item.put("name", def.getName());
            item.put("unit", def.getUnit());
            item.put("explanation", def.getTermExplanation());
            item.put("category", def.getCategory());
            result.put(def.getKey(), item);
        }
        return result;
    }

    @Override
    public String getExplanation(String indicatorKey) {
        IndicatorDefinition def = getDef(indicatorKey);
        return def != null ? def.getTermExplanation() : "";
    }

    @Override
    public int getSortOrder(String indicatorKey) {
        IndicatorDefinition def = getDef(indicatorKey);
        return def != null ? def.getSortOrder() : 99;
    }

    @Override
    public String getCategory(String indicatorKey) {
        IndicatorDefinition def = getDef(indicatorKey);
        return def != null ? def.getCategory() : "";
    }

    @Override
    public String getEvaluation(String indicatorKey, BigDecimal value) {
        if (value == null) return "—";
        return switch (indicatorKey) {
            case "debtRatio" -> {
                double v = value.doubleValue();
                if (v < 30) yield "优秀";
                if (v < 50) yield "良好";
                if (v < 70) yield "关注";
                yield "偏高";
            }
            case "grossMargin" -> {
                double v = value.doubleValue();
                if (v > 60) yield "优秀";
                if (v > 30) yield "良好";
                if (v > 15) yield "一般";
                yield "关注";
            }
            case "roe", "netMargin" -> {
                double v = value.doubleValue();
                if (v > 20) yield "优秀";
                if (v > 10) yield "良好";
                if (v > 5) yield "一般";
                yield "关注";
            }
            default -> {
                if (value.compareTo(BigDecimal.ZERO) > 0) yield "正常";
                else if (value.compareTo(BigDecimal.ZERO) < 0) yield "关注";
                else yield "—";
            }
        };
    }
}
