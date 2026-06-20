import math
import re
from dataclasses import dataclass
from pathlib import Path

import fitz


@dataclass
class RagContext:
    id: str
    content: str
    source: str
    score: float
    page: int | None = None


class RagService:
    def __init__(self, report_dir: str = "/app/data/reports"):
        self.report_dir = Path(report_dir)
        self.industry_knowledge = [
            ("profit_quality", "行业常识：利润质量要看净利润能否转化为经营现金流。现金流长期低于净利润，可能说明回款慢、应收账款压力大。"),
            ("debt", "行业常识：资产负债率可以理解为公司用了多少外部资金。重资产行业通常更高，轻资产行业通常更低，需要和同行比较。"),
            ("growth", "行业常识：收入增长像店铺客流和订单规模，利润增长像真正留下的钱。收入涨但利润不涨，要关注成本和费用压力。"),
            ("margin", "行业常识：毛利率反映产品或服务的赚钱空间。毛利率下降，常见原因包括降价、成本上涨或竞争加剧。"),
            ("risk", "行业常识：财报风险通常从增速放缓、毛利率下滑、负债升高、现金流变差和存货应收异常增加几个方向观察。"),
        ]

    def search(self, query: str, company_code: str, top_k: int = 5) -> list[RagContext]:
        candidates = self._knowledge_candidates(query)
        candidates.extend(self._report_candidates(query, company_code, max(top_k * 4, 12)))
        ranked = sorted(candidates, key=lambda item: item.score, reverse=True)
        return ranked[:top_k]

    def _knowledge_candidates(self, query: str) -> list[RagContext]:
        query_tokens = self._tokens(query)
        results = []
        for key, content in self.industry_knowledge:
            score = self._score(query_tokens, content)
            if score > 0:
                results.append(RagContext(
                    id=f"knowledge-{key}",
                    content=content,
                    source="财报行业常识库",
                    score=score,
                ))
        return results

    def _report_candidates(self, query: str, company_code: str, limit: int) -> list[RagContext]:
        company_dir = self.report_dir / company_code
        if not company_dir.exists():
            return []

        query_tokens = self._tokens(query)
        results = []
        for pdf_path in sorted(company_dir.glob("*.pdf"), key=lambda path: path.stat().st_mtime, reverse=True)[:6]:
            try:
                with fitz.open(pdf_path) as doc:
                    for page_index, page in enumerate(doc):
                        text = self._clean_text(page.get_text())
                        if not text:
                            continue
                        score = self._score(query_tokens, text)
                        if score <= 0:
                            continue
                        snippet = self._snippet(text, query_tokens)
                        results.append(RagContext(
                            id=f"{company_code}-{pdf_path.stem}-{page_index + 1}",
                            content=snippet,
                            source=f"{pdf_path.name} 第{page_index + 1}页",
                            score=score,
                            page=page_index + 1,
                        ))
                        if len(results) >= limit:
                            return results
            except Exception:
                continue
        return results

    def _tokens(self, text: str) -> list[str]:
        aliases = {
            "盈利": ["净利润", "毛利率", "利润"],
            "赚钱": ["净利润", "毛利率", "利润"],
            "风险": ["风险", "负债", "现金流", "下降"],
            "现金流": ["现金流", "经营活动产生的现金流量净额"],
            "负债": ["负债", "资产负债率"],
            "收入": ["营业收入", "营收"],
        }
        tokens = set(re.findall(r"[\u4e00-\u9fa5A-Za-z0-9]{2,}", text))
        for token, extras in aliases.items():
            if token in text:
                tokens.add(token)
                for extra in extras:
                    tokens.add(extra)
        for token in list(tokens):
            for extra in aliases.get(token, []):
                tokens.add(extra)
        return list(tokens)

    def _score(self, query_tokens: list[str], text: str) -> float:
        if not query_tokens:
            return 0
        hits = sum(1 for token in query_tokens if token in text)
        if hits == 0:
            return 0
        density = hits / math.sqrt(max(len(text), 1))
        return round(min(0.99, 0.55 + density * 4), 4)

    def _snippet(self, text: str, query_tokens: list[str]) -> str:
        positions = [text.find(token) for token in query_tokens if token in text]
        start = max(0, min(positions) - 120) if positions else 0
        return text[start:start + 520].strip()

    def _clean_text(self, text: str) -> str:
        return re.sub(r"\s+", " ", text or "").strip()


rag_service = RagService()
