package com.smartreport.repository;

import com.smartreport.models.entity.FinancialIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialIndicatorRepository extends JpaRepository<FinancialIndicator, Long> {

    List<FinancialIndicator> findByReportId(Long reportId);

    List<FinancialIndicator> findByReportIdIn(List<Long> reportIds);

    @Query("SELECT fi FROM FinancialIndicator fi JOIN FinancialReport fr ON fi.reportId = fr.id " +
           "WHERE fr.companyCode = :companyCode AND fi.indicatorKey = :key " +
           "ORDER BY fr.reportYear DESC")
    List<FinancialIndicator> findByCompanyCodeAndKeyOrderByYearDesc(
            @Param("companyCode") String companyCode, @Param("key") String key);
}
