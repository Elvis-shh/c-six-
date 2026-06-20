package com.smartreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartreport.models.dto.ChatModels.GenerateRequest;
import com.smartreport.models.dto.ChatModels.RagContext;
import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.ReportQuoteChunk;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.ReportQuoteChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final AiEngineClient aiEngineClient;
    private final CompanyRepository companyRepository;
    private final ReportQuoteChunkRepository quoteChunkRepository;
    private final ObjectMapper objectMapper;

    public SseEmitter sendMessage(String companyCode, String message, String sessionId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        CompletableFuture.runAsync(() -> streamAnswer(companyCode, message, emitter));
        return emitter;
    }

    private void streamAnswer(String companyCode, String message, SseEmitter emitter) {
        try {
            Company company = companyRepository.findById(companyCode).orElse(null);
            List<RagContext> contexts = searchContexts(message, companyCode);
            sendJson(emitter, "thinking", "正在检索财报上下文...");

            GenerateRequest request = GenerateRequest.builder()
                    .companyCode(companyCode)
                    .companyName(company != null ? company.getName() : companyCode)
                    .industry(company != null ? company.getIndustry() : "未知行业")
                    .message(message)
                    .ragContext(contexts)
                    .build();

            aiEngineClient.generateStream(request, line -> sendRaw(emitter, line));
            emitter.complete();
        } catch (Exception e) {
            try {
                sendJson(emitter, "token", fallbackReply(message));
                sendJson(emitter, "done", "");
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    private void sendRaw(SseEmitter emitter, String line) {
        try {
            String payload = line.startsWith("data: ") ? line.substring(6) : line;
            emitter.send(SseEmitter.event().data(payload));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void sendJson(SseEmitter emitter, String type, Object content) throws IOException {
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(new StreamEvent(type, content))));
    }

    private List<RagContext> searchContexts(String message, String companyCode) {
        List<RagContext> dbContexts = searchDatabaseContexts(message, companyCode, 5);
        if (!dbContexts.isEmpty()) {
            return dbContexts;
        }
        return aiEngineClient.search(message, companyCode, 5);
    }

    private List<RagContext> searchDatabaseContexts(String message, String companyCode, int topK) {
        List<String> tokens = extractTokens(message);
        if (tokens.isEmpty()) {
            return List.of();
        }
        List<RagContext> results = new ArrayList<>();
        for (ReportQuoteChunk chunk : quoteChunkRepository.findByCompanyCodeForRag(companyCode)) {
            int hits = 0;
            for (String token : tokens) {
                if (chunk.getContent().contains(token)) {
                    hits++;
                }
            }
            if (hits == 0) {
                continue;
            }
            double score = Math.min(0.99, 0.55 + hits * 0.12);
            results.add(RagContext.builder()
                    .id(String.valueOf(chunk.getId()))
                    .content(chunk.getContent())
                    .source(chunk.getSourceName())
                    .page(chunk.getPageNo())
                    .score(score)
                    .build());
        }
        return results.stream()
                .sorted(Comparator.comparing(RagContext::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private List<String> extractTokens(String message) {
        List<String> tokens = new ArrayList<>();
        for (String token : List.of("现金流", "经营活动产生的现金流量净额", "营业收入", "营业总收入", "净利润", "归属于上市公司股东的净利润", "资产总额", "资产总计", "负债合计", "负债总额", "毛利率", "风险", "负债")) {
            if (message.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String fallbackReply(String message) {
        if (message.contains("风险")) {
            return "AI 引擎暂时不可用。可先关注资产负债率、现金流波动、收入增速放缓等风险指标。";
        }
        if (message.contains("现金流")) {
            return "AI 引擎暂时不可用。现金流建议结合经营活动现金流净额与净利润匹配度一起判断。";
        }
        return "AI 引擎暂时不可用。请稍后重试，或先查看页面中的 KPI、趋势图和风险亮点。";
    }

    private record StreamEvent(String type, Object content) {}
}
