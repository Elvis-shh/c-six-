package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_crawl_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCrawlTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false, length = 10)
    private String companyCode;

    @Column(name = "report_year", nullable = false)
    private Integer reportYear;

    @Column(name = "report_type", nullable = false, length = 30)
    private String reportType;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "announcement_title", length = 255)
    private String announcementTitle;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "error_msg", length = 1000)
    private String errorMsg;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
