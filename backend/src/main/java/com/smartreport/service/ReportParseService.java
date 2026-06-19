package com.smartreport.service;

import com.smartreport.models.dto.ParseDtos.ParseStartRequest;
import com.smartreport.models.dto.ParseDtos.ParseStartResponse;
import com.smartreport.models.dto.ParseDtos.ParseStatusResponse;

public interface ReportParseService {
    ParseStartResponse start(ParseStartRequest request);
    void runPendingAsync();
    ParseStatusResponse status();
}
