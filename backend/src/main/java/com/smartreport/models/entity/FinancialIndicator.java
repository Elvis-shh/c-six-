package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "indicator_key", nullable = false, length = 50)
    private String indicatorKey;

    @Column(precision = 20, scale = 4)
    private BigDecimal value;

    @Column(name = "yoy_change", precision = 10, scale = 4)
    private BigDecimal yoyChange;

    @Column(name = "qoq_change", precision = 10, scale = 4)
    private BigDecimal qoqChange;

    @Column(name = "industry_avg", length = 50)
    private String industryAvg;

    @Column(name = "industry_rank", length = 50)
    private String industryRank;

    @Column(length = 20)
    private String rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
