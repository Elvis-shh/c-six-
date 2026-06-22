package com.smartreport.service.impl;

import com.smartreport.models.dto.CompanyBrief;
import com.smartreport.models.entity.Company;
import com.smartreport.repository.CompanyIndustryTagRepository;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final CompanyRepository companyRepository;
    private final CompanyIndustryTagRepository companyIndustryTagRepository;

    @Override
    public List<CompanyBrief> searchCompanies(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        List<String> a50Codes = companyIndustryTagRepository.findCompanyCodesByTag("中证A50");
        if (a50Codes.isEmpty()) {
            return List.of();
        }
        return companyRepository
                .searchCompaniesInCodes(keyword.trim(), a50Codes, PageRequest.of(0, Math.min(limit, 20)))
                .stream()
                .map(CompanyBrief::from)
                .toList();
    }

    @Override
    public List<CompanyBrief> getHotCompanies() {
        List<String> a50Codes = companyIndustryTagRepository.findCompanyCodesByTag("中证A50");
        List<Company> companies = new ArrayList<>(companyRepository.findByCodeIn(a50Codes));
        Collections.shuffle(companies);
        return companies.stream()
                .limit(6)
                .map(CompanyBrief::from)
                .toList();
    }
}
