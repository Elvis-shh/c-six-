package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_quote_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportQuoteChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "company_code", nullable = false, length = 10)
    private String companyCode;

    @Column(name = "report_year", nullable = false)
    private Integer reportYear;

    @Column(name = "page_no", nullable = false)
    private Integer pageNo;

    @Column(name = "source_name", nullable = false, length = 255)
    private String sourceName;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
