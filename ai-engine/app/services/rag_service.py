from dataclasses import dataclass


@dataclass
class RagContext:
    id: str
    content: str
    source: str
    score: float


class RagService:
    def search(self, query: str, company_code: str, top_k: int = 5) -> list[RagContext]:
        snippets = [
            "营业收入、归母净利润、毛利率、资产负债率、经营现金流是判断公司经营质量的核心指标。",
            "若经营现金流持续高于净利润，通常说明利润质量较高；若现金流显著弱于利润，需要关注回款风险。",
            "资产负债率上升代表杠杆提升，需结合行业均值、现金流和盈利稳定性综合判断。",
            "收入和利润的同比增速可以反映成长性，毛利率和净利率反映盈利能力。",
            "风险分析应重点观察收入增速放缓、利润率下滑、负债率上升和现金流恶化。",
        ]
        keyword = query[:8] or "财报"
        results = []
        for index, text in enumerate(snippets[:top_k]):
            results.append(RagContext(
                id=f"{company_code}-mock-{index}",
                content=text,
                source=f"{company_code} 财报知识库-{keyword}",
                score=round(0.92 - index * 0.06, 4),
            ))
        return results


rag_service = RagService()
