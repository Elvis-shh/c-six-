package com.smartreport.repository;

import com.smartreport.models.entity.ReportQuoteChunk;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportQuoteChunkRepository extends JpaRepository<ReportQuoteChunk, Long> {
    @Modifying
    @Transactional
    void deleteByReportId(Long reportId);

    boolean existsByReportId(Long reportId);

    List<ReportQuoteChunk> findTop20ByCompanyCodeOrderByReportYearDescPageNoAsc(String companyCode);

    @Query("""
            SELECT c FROM ReportQuoteChunk c
            WHERE c.companyCode = :companyCode AND c.content LIKE CONCAT('%', :keyword, '%')
            ORDER BY c.reportYear DESC, c.pageNo ASC
            """)
    List<ReportQuoteChunk> searchForRag(@Param("companyCode") String companyCode,
                                        @Param("keyword") String keyword,
                                        org.springframework.data.domain.Pageable pageable);
}
