package com.smartreport.service;

import com.smartreport.models.dto.CrawlerDtos.CrawlStartRequest;
import com.smartreport.models.dto.CrawlerDtos.CrawlStartResponse;
import com.smartreport.models.dto.CrawlerDtos.CrawlerStatusResponse;

public interface ReportCrawlerService {
    CrawlStartResponse start(CrawlStartRequest request);
    CrawlerStatusResponse status();
    void runPendingAsync();
}
