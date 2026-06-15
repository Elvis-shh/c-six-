package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "indicator_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorDefinition {

    @Id
    @Column(length = 50)
    private String key;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String unit;

    @Column(name = "is_percentage", nullable = false)
    @Builder.Default
    private Integer isPercentage = 0;

    @Column(length = 50)
    private String category;

    @Column(name = "term_explanation", columnDefinition = "TEXT")
    private String termExplanation;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
