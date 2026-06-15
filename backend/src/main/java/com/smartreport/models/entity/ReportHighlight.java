package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_highlights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_report_id", nullable = false)
    private Long analysisReportId;

    @Column(name = "rule_key", nullable = false, length = 50)
    private String ruleKey;

    @Column(length = 10)
    private String icon;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
