package com.smartreport.service;

import com.smartreport.models.dto.ChatModels.GenerateRequest;
import com.smartreport.models.dto.ChatModels.RagContext;
import com.smartreport.models.dto.ChatModels.RagSearchRequest;
import com.smartreport.models.dto.ChatModels.RagSearchResponse;
import com.smartreport.models.dto.ParseDtos.AiParseReportResponse;
import com.smartreport.models.dto.ParseDtos.AiParseReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
                .exchange((clientRequest, clientResponse) -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientResponse.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.isBlank()) {
                                onLine.accept(line);
                            }
                        }
                    }
                    return null;
                });
    }

    public AiParseReportResponse parseReport(String filePath) {
        return restClient.post()
                .uri("/ai/v1/reports/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiParseReportRequest(filePath))
                .retrieve()
                .body(AiParseReportResponse.class);
    }

    public Map<String, Object> batchFetchParse(String companyCode, int years) {
        Map<String, Object> body = Map.of("companyCode", companyCode, "years", years);
        return restClient.post()
                .uri("/ai/v1/reports/batch-fetch-parse")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
    }
}
