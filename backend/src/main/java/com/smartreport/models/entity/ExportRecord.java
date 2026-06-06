package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "export_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRecord {

    @Id
    @Column(length = 64)
    private String taskId;

    @Column(name = "company_code", length = 20)
    private String companyCode;

    @Column(nullable = false, length = 10)
    private String format;

    @Column(length = 50)
    private String status; // pending, rendering, ready, failed

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Builder.Default
    private Integer progress = 0;

    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
