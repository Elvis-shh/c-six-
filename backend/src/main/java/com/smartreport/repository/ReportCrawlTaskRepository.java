package com.smartreport.repository;

import com.smartreport.models.entity.ReportCrawlTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportCrawlTaskRepository extends JpaRepository<ReportCrawlTask, Long> {
    Optional<ReportCrawlTask> findByCompanyCodeAndReportYearAndReportType(String companyCode, Integer reportYear, String reportType);
    List<ReportCrawlTask> findTop50ByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
