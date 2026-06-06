package com.smartreport.service;

import com.smartreport.models.dto.BenchmarkResponse;

public interface BenchmarkService {

    BenchmarkResponse getBenchmark(String companyCode, int year);
}
