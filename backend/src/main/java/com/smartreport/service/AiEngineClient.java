package com.smartreport.service;

import com.smartreport.models.dto.ChatModels.GenerateRequest;
import com.smartreport.models.dto.ChatModels.RagContext;
import com.smartreport.models.dto.ChatModels.RagSearchRequest;
import com.smartreport.models.dto.ChatModels.RagSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class AiEngineClient {
    private final RestClient restClient;

    public AiEngineClient(@Value("${ai-engine.url:http://localhost:8000}") String aiEngineUrl) {
        this.restClient = RestClient.builder().baseUrl(aiEngineUrl).build();
    }

    public List<RagContext> search(String query, String companyCode, int topK) {
        try {
            RagSearchResponse response = restClient.post()
                    .uri("/ai/v1/rag/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(RagSearchRequest.builder().query(query).companyCode(companyCode).topK(topK).build())
                    .retrieve()
                    .body(RagSearchResponse.class);
            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("RAG search failed, using fallback context: {}", e.getMessage());
            return List.of();
        }
    }

    public void generateStream(GenerateRequest request, Consumer<String> onLine) {
        restClient.post()
                .uri("/ai/v1/chat/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(request)
                .retrieve()
                .body(String.class)
                .lines()
                .filter(line -> !line.isBlank())
                .forEach(onLine);
    }
}
