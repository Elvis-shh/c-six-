package com.smartreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartreport.models.dto.ChatModels.GenerateRequest;
import com.smartreport.models.dto.ChatModels.RagContext;
import com.smartreport.models.entity.Company;
import com.smartreport.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final AiEngineClient aiEngineClient;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    public SseEmitter sendMessage(String companyCode, String message, String sessionId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        CompletableFuture.runAsync(() -> streamAnswer(companyCode, message, emitter));
        return emitter;
    }

    private void streamAnswer(String companyCode, String message, SseEmitter emitter) {
        try {
            Company company = companyRepository.findById(companyCode).orElse(null);
            List<RagContext> contexts = aiEngineClient.search(message, companyCode, 5);
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

    private void sendJson(SseEmitter emitter, String type, String content) throws IOException {
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(new StreamEvent(type, content))));
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

    private record StreamEvent(String type, String content) {}
}
