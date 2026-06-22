package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.models.dto.KnowledgeDtos.BuildKnowledgeRequest;
import com.smartreport.models.dto.KnowledgeDtos.BuildKnowledgeResponse;
import com.smartreport.models.dto.KnowledgeDtos.KnowledgeSearchRequest;
import com.smartreport.models.dto.KnowledgeDtos.KnowledgeSearchResponse;
import com.smartreport.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    @PostMapping("/build")
    public ApiResponse<BuildKnowledgeResponse> build(@RequestBody BuildKnowledgeRequest request) {
        return ApiResponse.accepted(knowledgeService.build(request));
    }

    @PostMapping("/search")
    public ApiResponse<KnowledgeSearchResponse> search(@RequestBody KnowledgeSearchRequest request) {
        return ApiResponse.success(knowledgeService.search(request));
    }
}
