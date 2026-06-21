package com.smartreport.service;

import com.smartreport.models.dto.KnowledgeDtos.BuildKnowledgeRequest;
import com.smartreport.models.dto.KnowledgeDtos.BuildKnowledgeResponse;
import com.smartreport.models.dto.KnowledgeDtos.KnowledgeSearchRequest;
import com.smartreport.models.dto.KnowledgeDtos.KnowledgeSearchResponse;

public interface KnowledgeService {
    BuildKnowledgeResponse build(BuildKnowledgeRequest request);
    KnowledgeSearchResponse search(KnowledgeSearchRequest request);
}
