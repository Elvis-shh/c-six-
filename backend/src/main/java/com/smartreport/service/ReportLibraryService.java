package com.smartreport.service;

import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportLibraryService {
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final CompanyRepository companyRepository;

    public List<Map<String, Object>> list() {
        List<FinancialReport> reports = reportRepository.findAllByOrderByCompanyCodeAscReportYearDescIdDesc();
        Map<String, Company> companies = companyRepository.findByCodeIn(
                        reports.stream().map(FinancialReport::getCompanyCode).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Company::getCode, Function.identity()));

        return reports.stream().map(report -> {
            Company company = companies.get(report.getCompanyCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", report.getId());
            item.put("companyCode", report.getCompanyCode());
            item.put("companyName", company != null ? company.getName() : report.getCompanyCode());
            item.put("reportYear", report.getReportYear());
            item.put("reportType", report.getReportType());
            item.put("source", report.getSource());
            item.put("sourceLabel", "upload".equals(report.getSource()) ? "用户上传" : "系统内置");
            item.put("deletable", "upload".equals(report.getSource()));
            item.put("createdAt", report.getCreatedAt());
            return item;
        }).toList();
    }

    public void deleteUploadedReport(Long reportId) {
        FinancialReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("财报不存在"));
        if (!"upload".equals(report.getSource())) {
            throw new IllegalArgumentException("系统内置财报不能删除");
        }
        indicatorRepository.deleteByReportId(report.getId());
        reportRepository.delete(report);
    }
}
