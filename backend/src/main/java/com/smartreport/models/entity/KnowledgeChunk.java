package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String domain;

    @Column(name = "company_code", length = 10)
    private String companyCode;

    @Column(nullable = false, length = 50)
    private String sourceType;

    @Column(nullable = false, length = 500)
    private String sourceName;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "page_no")
    private Integer pageNo;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    @Column(name = "embedding_json")
    private String embeddingJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
