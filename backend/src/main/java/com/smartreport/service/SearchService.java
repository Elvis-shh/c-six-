package com.smartreport.service;

import com.smartreport.models.dto.CompanyBrief;

import java.util.List;

public interface SearchService {

    List<CompanyBrief> searchCompanies(String keyword, int limit);

    List<CompanyBrief> getHotCompanies();
}
