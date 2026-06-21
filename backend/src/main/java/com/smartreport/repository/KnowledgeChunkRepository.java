package com.smartreport.repository;

import com.smartreport.models.entity.KnowledgeChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    @Query("""
            SELECT k FROM KnowledgeChunk k
            WHERE k.domain = :domain
              AND (:companyCode IS NULL OR k.companyCode = :companyCode OR k.companyCode IS NULL)
              AND k.content LIKE CONCAT('%', :keyword, '%')
            ORDER BY k.companyCode DESC, k.id DESC
            """)
    List<KnowledgeChunk> search(@Param("domain") String domain,
                                @Param("companyCode") String companyCode,
                                @Param("keyword") String keyword,
                                Pageable pageable);
}
