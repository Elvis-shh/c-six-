package com.smartreport.service;

import com.smartreport.models.entity.IndicatorDefinition;
import com.smartreport.repository.IndicatorDefinitionRepository;
import com.smartreport.service.impl.IndicatorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("IndicatorService 指标计算引擎")
class IndicatorServiceTest {

    private IndicatorServiceImpl indicatorService;

    @BeforeEach
    void setUp() {
        var repo = mock(IndicatorDefinitionRepository.class);
        when(repo.findById("revenue")).thenReturn(java.util.Optional.of(def("revenue", "营业总收入", "亿", 0)));
        when(repo.findById("profit")).thenReturn(java.util.Optional.of(def("profit", "归母净利润", "亿", 0)));
        when(repo.findById("debtRatio")).thenReturn(java.util.Optional.of(def("debtRatio", "资产负债率", "%", 1)));
        when(repo.findById("cashFlow")).thenReturn(java.util.Optional.of(def("cashFlow", "经营现金流", "亿", 0)));
        when(repo.findById("grossMargin")).thenReturn(java.util.Optional.of(def("grossMargin", "毛利率", "%", 1)));
        indicatorService = new IndicatorServiceImpl(repo);
    }

    private IndicatorDefinition def(String key, String name, String unit, int isPct) {
        return IndicatorDefinition.builder().key(key).name(name).unit(unit).isPercentage(isPct).build();
    }

    @Nested
    @DisplayName("calculateYoY 同比增长率")
    class YoYTests {
        @ParameterizedTest
        @CsvSource({"150,120,25.00", "80,100,-20.00", "100,200,-50.00", "200,100,100.00"})
        void normalCases(double current, double prev, double expected) {
            var result = indicatorService.calculateYoY(bd(current), bd(prev));
            assertThat(result).isEqualByComparingTo(bd(expected));
        }

        @Test
        void prevZeroReturnsNull() {
            assertThat(indicatorService.calculateYoY(bd(100), bd(0))).isNull();
        }

        @Test
        void nullInputReturnsNull() {
            assertThat(indicatorService.calculateYoY(null, bd(100))).isNull();
            assertThat(indicatorService.calculateYoY(bd(100), null)).isNull();
        }

        @Test
        void negativeToPositive() {
            var result = indicatorService.calculateYoY(bd(100), bd(-50));
            assertThat(result).isEqualByComparingTo(bd("300.00"));
        }
    }

    @Nested
    @DisplayName("calculateCAGR 复合增长率")
    class CAGRTest {
        @Test
        void threeYearCagr() {
            var vals = List.of(bd(100), bd(120), bd(144));
            var result = indicatorService.calculateCAGR(vals, 2);
            assertThat(result).isEqualByComparingTo(bd("20.00"));
        }

        @Test
        void fiveYearGrowth() {
            var vals = List.of(bd(980), bd(1095), bd(1276), bd(1506), bd(1748));
            var result = indicatorService.calculateCAGR(vals, 4);
            assertThat(result.doubleValue()).isGreaterThan(12).isLessThan(18);
        }

        @Test
        void negativeStartReturnsNull() {
            assertThat(indicatorService.calculateCAGR(List.of(bd(-100), bd(100)), 1)).isNull();
        }

        @Test
        void tooFewValuesReturnsNull() {
            assertThat(indicatorService.calculateCAGR(List.of(bd(100)), 1)).isNull();
        }
    }

    @Nested
    @DisplayName("calculateRatio 比率计算")
    class RatioTest {
        @Test
        void normalRatio() {
            assertThat(indicatorService.calculateRatio(bd(80), bd(100))).isEqualByComparingTo(bd("80.00"));
        }

        @Test
        void denominatorZeroReturnsNull() {
            assertThat(indicatorService.calculateRatio(bd(100), bd(0))).isNull();
        }
    }

    @Nested
    @DisplayName("determineTrend 趋势判断")
    class TrendTest {
        @Test
        void revenueUpIsUp() {
            assertThat(indicatorService.determineTrend("revenue", bd(16.1))).isEqualTo("up");
        }

        @Test
        void revenueDownIsDown() {
            assertThat(indicatorService.determineTrend("revenue", bd(-5.0))).isEqualTo("down");
        }

        @Test
        void debtRatioDownIsGood() {
            assertThat(indicatorService.determineTrend("debtRatio", bd(-1.1))).isEqualTo("down_good");
        }

        @Test
        void debtRatioUpIsDown() {
            assertThat(indicatorService.determineTrend("debtRatio", bd(5.0))).isEqualTo("down");
        }

        @Test
        void nullYoyIsNeutral() {
            assertThat(indicatorService.determineTrend("revenue", null)).isEqualTo("neutral");
        }
    }

    private static BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
