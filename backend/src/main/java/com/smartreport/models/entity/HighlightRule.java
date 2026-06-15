package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "highlight_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HighlightRule {

    @Id
    @Column(name = "rule_key", length = 50)
    private String ruleKey;

    @Column(name = "indicator_key", length = 50)
    private String indicatorKey;

    @Column(name = "condition_expr", length = 500)
    private String conditionExpr;

    @Column(length = 100)
    private String title;

    @Column(name = "desc_template", columnDefinition = "TEXT")
    private String descTemplate;

    @Column(length = 10)
    @Builder.Default
    private String icon = "✨";

    @Builder.Default
    private Integer priority = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer enabled = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
