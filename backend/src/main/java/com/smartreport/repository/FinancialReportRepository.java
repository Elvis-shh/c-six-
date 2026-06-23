package com.smartreport.repository;

import com.smartreport.models.entity.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, Long> {

    Optional<FinancialReport> findTopByCompanyCodeAndStatusOrderByReportYearDesc(String companyCode, Integer status);

    List<FinancialReport> findByCompanyCodeAndReportYearBetweenOrderByReportYearAsc(
            String companyCode, Integer startYear, Integer endYear);

    List<FinancialReport> findByCompanyCodeOrderByReportYearDesc(String companyCode);

    List<FinancialReport> findByCompanyCodeInAndSourceAndStatus(List<String> companyCodes, String source, Integer status);

    Optional<FinancialReport> findByCompanyCodeAndReportYearAndReportType(String companyCode, Integer reportYear, String reportType);

    @Query("SELECT fr FROM FinancialReport fr WHERE fr.companyCode = :companyCode AND fr.status = 1 ORDER BY fr.reportYear DESC, CASE WHEN fr.source = 'crawler' THEN 0 ELSE 1 END, fr.id DESC")
    List<FinancialReport> findActiveByCompanyCodeOrderForDisplay(@Param("companyCode") String companyCode);

    @Query("SELECT fr FROM FinancialReport fr WHERE fr.companyCode = :companyCode ORDER BY fr.reportYear DESC, CASE WHEN fr.status = 1 THEN 0 ELSE 1 END, CASE WHEN fr.source = 'crawler' THEN 0 ELSE 1 END, fr.id DESC")
    List<FinancialReport> findByCompanyCodeOrderForDisplayIncludingParsed(@Param("companyCode") String companyCode);

    boolean existsByCompanyCodeAndReportYearAndSource(String companyCode, Integer reportYear, String source);
}
