package com.smartreport.service.impl;

import com.smartreport.models.dto.CompanyBrief;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final CompanyRepository companyRepository;

    private static final List<String> HOT_COMPANY_CODES = List.of(
            "600519", "000858", "300750", "601398", "002415", "600276"
    );

    @Override
    public List<CompanyBrief> searchCompanies(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return companyRepository
                .searchCompanies(keyword.trim(), PageRequest.of(0, Math.min(limit, 20)))
                .stream()
                .map(CompanyBrief::from)
                .toList();
    }

    @Override
    public List<CompanyBrief> getHotCompanies() {
        return companyRepository.findByCodeIn(HOT_COMPANY_CODES)
                .stream()
                .map(CompanyBrief::from)
                .toList();
    }
}
