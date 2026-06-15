package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "industry_averages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndustryAverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String industry;

    @Column(name = "indicator_key", nullable = false, length = 50)
    private String indicatorKey;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "avg_value", precision = 20, scale = 4)
    private BigDecimal avgValue;

    @Column(name = "median_value", precision = 20, scale = 4)
    private BigDecimal medianValue;
}
