package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "company_code", nullable = false, length = 10)
    private String companyCode;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0;

    @Column(name = "summary_data", columnDefinition = "JSON")
    private String summaryData;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
