package com.smartreport.repository;

import com.smartreport.models.entity.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, Long> {

    Optional<FinancialReport> findTopByCompanyCodeAndStatusOrderByReportYearDesc(String companyCode, Integer status);

    List<FinancialReport> findByCompanyCodeAndReportYearBetweenOrderByReportYearAsc(
            String companyCode, Integer startYear, Integer endYear);

    List<FinancialReport> findByCompanyCodeOrderByReportYearDesc(String companyCode);

    Optional<FinancialReport> findByCompanyCodeAndReportYearAndReportType(String companyCode, Integer reportYear, String reportType);
}
