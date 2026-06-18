from collections.abc import AsyncGenerator

import json

import httpx

from app.core.config import settings
from app.core.prompts import SYSTEM_PROMPT


class LLMService:
    async def chat_stream(self, company_name: str, question: str, contexts: list[dict]) -> AsyncGenerator[str, None]:
        if settings.llm_provider.lower() == "deepseek" and settings.deepseek_api_key:
            try:
                async for token in self._deepseek_stream(company_name, question, contexts):
                    yield token
                return
            except Exception:
                # 生产上应接入结构化日志；这里保留兜底，避免 SSE 直接中断。
                pass

        for char in self._fallback_answer(company_name, question, contexts):
            yield char

    async def _deepseek_stream(self, company_name: str, question: str, contexts: list[dict]) -> AsyncGenerator[str, None]:
        url = f"{settings.deepseek_base_url.rstrip('/')}/chat/completions"
        headers = {
            "Authorization": f"Bearer {settings.deepseek_api_key}",
            "Content-Type": "application/json",
        }
        messages = self._build_messages(company_name, question, contexts)
        payload = {
            "model": settings.deepseek_model,
            "messages": messages,
            "temperature": 0.3,
            "max_tokens": 1200,
            "stream": True,
        }

        async with httpx.AsyncClient(timeout=45) as client:
            async with client.stream("POST", url, headers=headers, json=payload) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if not line.startswith("data: "):
                        continue
                    data = line[6:].strip()
                    if data == "[DONE]":
                        break
                    try:
                        chunk = json.loads(data)
                    except json.JSONDecodeError:
                        continue
                    delta = chunk.get("choices", [{}])[0].get("delta", {})
                    content = delta.get("content")
                    if content:
                        yield content

    def _build_messages(self, company_name: str, question: str, contexts: list[dict]) -> list[dict]:
        context_text = "\n\n".join(
            f"[来源: {item.get('source', '财报上下文')}]\n{item.get('content', '')}"
            for item in contexts[:5]
        ) or "暂无可用检索上下文。"

        system_prompt = SYSTEM_PROMPT.format(
            company_name=company_name,
            company_code="当前公司",
            industry="财报分析",
        )
        user_prompt = f"""## 参考数据
{context_text}

## 用户问题
{question}

请基于参考数据回答。如果参考数据不足，请明确说明不足，不要编造具体财报数值。"""
        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

    def _fallback_answer(self, company_name: str, question: str, contexts: list[dict]) -> str:
        context_text = "；".join(item.get("content", "") for item in contexts[:3])
        if "风险" in question:
            return f"DeepSeek 暂时不可用。{company_name} 的风险可从收入增速、利润率、资产负债率和现金流四个方向观察。参考上下文显示：{context_text}。"
        elif "现金流" in question:
            return f"DeepSeek 暂时不可用。{company_name} 的现金流需要和净利润一起看。若经营现金流持续覆盖净利润，利润质量更稳；若显著低于净利润，则要关注回款和营运资金压力。参考：{context_text}。"
        elif "盈利" in question or "赚钱" in question:
            return f"DeepSeek 暂时不可用。{company_name} 的盈利能力建议重点看营收增长、净利润增长、毛利率和净利率。参考上下文提示：{context_text}。"
        return f"DeepSeek 暂时不可用。我会基于财报上下文回答：{context_text}。你的问题是“{question}”，建议结合 KPI、趋势图、行业对比和风险提示一起判断。"


llm_service = LLMService()
