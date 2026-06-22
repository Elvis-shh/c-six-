package com.smartreport.repository;

import com.smartreport.models.entity.FinancialIndicator;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialIndicatorRepository extends JpaRepository<FinancialIndicator, Long> {

    List<FinancialIndicator> findByReportId(Long reportId);

    List<FinancialIndicator> findByReportIdIn(List<Long> reportIds);

    @Modifying
    @Transactional
    void deleteByReportId(Long reportId);

    @Query("SELECT fi FROM FinancialIndicator fi JOIN FinancialReport fr ON fi.reportId = fr.id " +
           "WHERE fr.companyCode = :companyCode AND fi.indicatorKey = :key " +
           "ORDER BY fr.reportYear DESC")
    List<FinancialIndicator> findByCompanyCodeAndKeyOrderByYearDesc(
            @Param("companyCode") String companyCode, @Param("key") String key);

    @Query("""
            SELECT fi.indicatorKey
            FROM FinancialIndicator fi
            JOIN FinancialReport fr ON fi.reportId = fr.id
            JOIN Company c ON fr.companyCode = c.code
            WHERE c.industry = :industry AND fr.source = :source AND fr.status = 1
            GROUP BY fi.indicatorKey
            ORDER BY COUNT(DISTINCT fr.id) DESC
            """)
    List<String> findMostCoveredKeysByIndustryAndSource(@Param("industry") String industry,
                                                        @Param("source") String source,
                                                        org.springframework.data.domain.Pageable pageable);
}
