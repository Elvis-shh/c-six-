package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false, length = 10)
    private String companyCode;

    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType;

    @Column(name = "report_year", nullable = false)
    private Integer reportYear;

    @Column(length = 20)
    @Builder.Default
    private String source = "system";

    @Column(name = "source_file_url", length = 500)
    private String sourceFileUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
