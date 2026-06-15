package com.smartreport.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IndicatorService {

    /** 计算同比增长率 (百分比). 除零/空值 返回 null */
    BigDecimal calculateYoY(BigDecimal current, BigDecimal previous);

    /** 计算环比增长率 */
    BigDecimal calculateQoQ(BigDecimal current, BigDecimal previous);

    /** 计算复合年增长率 (CAGR) */
    BigDecimal calculateCAGR(List<BigDecimal> values, int years);

    /** 计算比率 (百分比). 分母为0返回null */
    BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator);

    /** 判断趋势方向: up/down/down_good */
    String determineTrend(String indicatorKey, BigDecimal yoy);

    /** 获取指标单位 */
    String getUnit(String indicatorKey);

    /** 获取指标中文名 */
    String getName(String indicatorKey);

    /** 获取所有指标定义 */
    Map<String, Map<String, String>> getAllDefinitions();

    /** 获取指标评价 */
    String getEvaluation(String indicatorKey, BigDecimal value);

    /** 获取指标分类 */
    String getCategory(String indicatorKey);

    /** 获取指标解释 */
    String getExplanation(String indicatorKey);

    /** 获取指标排序 */
    int getSortOrder(String indicatorKey);
}
