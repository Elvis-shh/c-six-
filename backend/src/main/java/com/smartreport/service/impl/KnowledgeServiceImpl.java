package com.smartreport.service.impl;

import com.smartreport.models.dto.KnowledgeDtos.*;
import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.models.entity.KnowledgeChunk;
import com.smartreport.models.entity.ReportQuoteChunk;
import com.smartreport.repository.CompanyIndustryTagRepository;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.FinancialReportRepository;
import com.smartreport.repository.KnowledgeChunkRepository;
import com.smartreport.repository.ReportQuoteChunkRepository;
import com.smartreport.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {
    private final CompanyRepository companyRepository;
    private final CompanyIndustryTagRepository companyIndustryTagRepository;
    private final FinancialReportRepository reportRepository;
    private final ReportQuoteChunkRepository quoteChunkRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    @Override
    @Transactional
    public BuildKnowledgeResponse build(BuildKnowledgeRequest request) {
        List<String> codes = targetCodes(request.getIndexCode());
        List<FinancialReport> reports = reportRepository.findByCompanyCodeInAndSourceAndStatus(codes, "crawler", 1);
        int imported = 0;
        int skipped = 0;
        for (FinancialReport report : reports.stream().limit(normalizeLimit(request.getLimit())).toList()) {
            List<ReportQuoteChunk> chunks = quoteChunkRepository.findByReportIdOrderByPageNoAsc(report.getId());
            if (chunks.isEmpty()) {
                skipped++;
                continue;
            }
            for (ReportQuoteChunk chunk : chunks) {
                if (chunk.getContent() == null || chunk.getContent().isBlank()) {
                    continue;
                }
                knowledgeChunkRepository.save(KnowledgeChunk.builder()
                        .domain("financial-report")
                        .companyCode(chunk.getCompanyCode())
                        .sourceType("annual_report")
                        .sourceName(chunk.getSourceName())
                        .pageNo(chunk.getPageNo())
                        .content(chunk.getContent())
                        .embeddingJson(pseudoEmbedding(chunk.getContent()))
                        .build());
                imported++;
            }
        }
        if (Boolean.TRUE.equals(request.getIncludeWeb())) {
            imported += importWebKnowledge();
        }
        return BuildKnowledgeResponse.builder().imported(imported).skipped(skipped).build();
    }

    @Override
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        int topK = request.getTopK() == null || request.getTopK() <= 0 ? 8 : Math.min(request.getTopK(), 30);
        List<String> keywords = keywords(request.getQuery());
        Map<Long, KnowledgeSearchItem> merged = new LinkedHashMap<>();
        for (String keyword : keywords) {
            knowledgeChunkRepository.search("financial-report", request.getCompanyCode(), keyword, PageRequest.of(0, topK * 4))
                    .forEach(chunk -> {
                        double candidateScore = score(request.getQuery(), chunk.getContent());
                        KnowledgeSearchItem current = merged.get(chunk.getId());
                        if (current == null || candidateScore > current.getScore()) {
                            merged.put(chunk.getId(), KnowledgeSearchItem.builder()
                                    .id(chunk.getId())
                                    .sourceType(chunk.getSourceType())
                                    .sourceName(chunk.getSourceName())
                                    .sourceUrl(chunk.getSourceUrl())
                                    .companyCode(chunk.getCompanyCode())
                                    .pageNo(chunk.getPageNo())
                                    .content(snippet(chunk.getContent(), keyword))
                                    .score(candidateScore)
                                    .build());
                        }
                    });
        }
        List<KnowledgeSearchItem> results = merged.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .toList();
        return KnowledgeSearchResponse.builder().results(results).build();
    }

    private List<String> targetCodes(String indexCode) {
        if ("CSI_A50".equalsIgnoreCase(indexCode) || "A50".equalsIgnoreCase(indexCode) || "000510".equals(indexCode)) {
            return companyIndustryTagRepository.findCompanyCodesByTag("中证A50");
        }
        return List.of();
    }

    private int importWebKnowledge() {
        List<KnowledgeChunk> items = new ArrayList<>();
        items.add(webItem("核心指标", "财报分析通常优先看营业收入、归母净利润、经营现金流、净利率、资产负债率、ROE、研发费用率和每股收益。收入看规模，利润看结果，现金流看质量，负债看风险，ROE看股东回报。"));
        items.add(webItem("利润质量", "经营现金流长期低于净利润时，要关注应收账款、存货和收入确认质量。经营现金流高于或接近净利润，通常说明利润含金量较好。"));
        items.add(webItem("成长分析", "营收增长和净利润增长需要同时观察。营收增长但净利润不增长，可能意味着成本、费用或竞争压力上升。净利润增长但收入停滞，则要关注一次性收益。"));
        items.add(webItem("财务风险", "资产负债率需要结合行业比较。银行、地产等行业天然较高，消费、医药、软件等轻资产行业通常较低。负债率上升时要同步看现金流和短债压力。"));
        items.add(webItem("研发投入", "研发费用率适合观察科技、医药、新能源、高端制造公司的长期竞争力。研发费用率不能只看越高越好，还要结合收入增速和商业化效率。"));
        knowledgeChunkRepository.saveAll(items);
        return items.size();
    }

    private KnowledgeChunk webItem(String title, String content) {
        return KnowledgeChunk.builder()
                .domain("financial-report")
                .sourceType("web_knowledge")
                .sourceName(title)
                .sourceUrl("https://www.csindex.com.cn/")
                .content(content)
                .embeddingJson(pseudoEmbedding(content))
                .build();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 200;
        }
        return Math.min(limit, 5000);
    }

    private String bestKeyword(String query) {
        if (query == null || query.isBlank()) {
            return "财报";
        }
        String compact = query.replaceAll("\\s+", "");
        if (compact.length() <= 8) {
            return compact;
        }
        return compact.substring(0, 8);
    }

    private List<String> keywords(String query) {
        List<String> keywords = new ArrayList<>();
        if (query != null) {
            for (String token : query.replaceAll("[^\\p{IsHan}A-Za-z0-9]", " ").split("\\s+")) {
                if (!token.isBlank() && token.length() >= 2 && !keywords.contains(token)) {
                    keywords.add(token);
                }
            }
        }
        if (keywords.isEmpty()) {
            keywords.add(bestKeyword(query));
        }
        return keywords;
    }

    private String snippet(String content, String keyword) {
        int index = content.indexOf(keyword);
        int start = index < 0 ? 0 : Math.max(0, index - 120);
        return content.substring(start, Math.min(content.length(), start + 600));
    }

    private double score(String query, String content) {
        if (query == null || query.isBlank() || content == null) {
            return 0.5;
        }
        int hits = 0;
        for (String token : query.replaceAll("[^\\p{IsHan}A-Za-z0-9]", " ").split("\\s+")) {
            if (!token.isBlank() && content.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT))) {
                hits++;
            }
        }
        return Math.min(0.99, 0.55 + hits * 0.1);
    }

    private String pseudoEmbedding(String content) {
        int[] buckets = new int[16];
        String text = content == null ? "" : content;
        for (int index = 0; index < text.length(); index++) {
            buckets[index % buckets.length] += text.charAt(index);
        }
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < buckets.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.4f", (buckets[index] % 1000) / 1000.0));
        }
        return builder.append(']').toString();
    }
}
