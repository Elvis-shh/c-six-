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

请基于参考数据回答。如果参考数据不足，请明确说明不足，不要编造具体财报数值。
只有参考数据里明确出现过的数字才能写进答案；没有把握时，改用定性描述，不要硬给具体数字。
不要在回答末尾列出参考资料或引用来源，引用会由界面单独展示。
"""
        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

    def _fallback_answer(self, company_name: str, question: str, contexts: list[dict]) -> str:
        if "风险" in question:
            return f"DeepSeek 暂时不可用。\n\n**怎么看**：{company_name} 的风险可以先看收入有没有放慢、利润率有没有下降、负债有没有升高、现金流有没有变弱。\n\n**为什么**：这些指标就像一家店的客流、净赚的钱、借款压力和真正收到的钱。"
        elif "现金流" in question:
            return f"DeepSeek 暂时不可用。\n\n**怎么看**：现金流要和净利润一起看。\n\n**为什么**：净利润像账面赚的钱，经营现金流更像真正收回来的钱。"
        elif "盈利" in question or "赚钱" in question:
            return f"DeepSeek 暂时不可用。\n\n**怎么看**：盈利能力先看收入、净利润和毛利率。\n\n**为什么**：收入像生意规模，净利润像最后留下的钱，毛利率像每单生意的赚钱空间。"
        return f"DeepSeek 暂时不可用。\n\n**怎么看**：我会先按财报原文和行业常识回答你的问题。\n\n**为什么**：这样可以减少凭空猜测。"

llm_service = LLMService()
