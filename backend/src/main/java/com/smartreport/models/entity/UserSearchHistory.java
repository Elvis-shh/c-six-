package com.smartreport.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_search_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "company_code", nullable = false, length = 10)
    private String companyCode;

    @Column(name = "searched_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime searchedAt = LocalDateTime.now();
}
