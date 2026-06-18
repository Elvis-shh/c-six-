from collections.abc import AsyncGenerator


class LLMService:
    async def chat_stream(self, company_name: str, question: str, contexts: list[dict]) -> AsyncGenerator[str, None]:
        context_text = "；".join(item.get("content", "") for item in contexts[:3])
        if "风险" in question:
            answer = f"{company_name} 的风险可从收入增速、利润率、资产负债率和现金流四个方向观察。参考上下文显示：{context_text}。建议优先核对页面中的风险提示和近五年趋势。"
        elif "现金流" in question:
            answer = f"{company_name} 的现金流需要和净利润一起看。若经营现金流持续覆盖净利润，利润质量更稳；若显著低于净利润，则要关注回款和营运资金压力。参考：{context_text}。"
        elif "盈利" in question or "赚钱" in question:
            answer = f"{company_name} 的盈利能力建议重点看营收增长、净利润增长、毛利率和净利率。参考上下文提示：{context_text}。页面的 KPI 卡片和指标详解表可以进一步验证具体数值。"
        else:
            answer = f"我会基于财报上下文回答：{context_text}。你的问题是“{question}”，建议结合 KPI、趋势图、行业对比和风险提示一起判断。数据仅供参考，不构成投资建议。"

        for char in answer:
            yield char


llm_service = LLMService()
