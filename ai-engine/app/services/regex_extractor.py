import re


class RegexExtractor:
    INDICATOR_ALIASES = {
        "revenue": ["营业收入", "营业总收入", "營業收入", "營業總收入"],
        "profit": ["归属于上市公司股东的净利润", "归属于母公司股东的净利润", "归母净利润", "歸屬於上市公司股東的淨利潤", "歸屬於母公司股東的淨利潤"],
        "totalAssets": ["资产总额", "资产总计", "总资产", "資產總額", "資產總計", "總資產"],
        "totalLiabilities": ["负债合计", "负债总额", "总负债", "負債合計", "負債總額", "總負債"],
        "cashFlow": ["经营活动产生的现金流量净额", "经营活动现金流量净额", "經營活動產生的現金流量淨額", "經營活動現金流量淨額"],
    }

    MULTILINE_ALIASES = {
        "profit": [["归属于上市公司股东", "的净利润"], ["归属于母公司股东", "的净利润"], ["歸屬於上市公司股東", "的淨利潤"], ["歸屬於母公司股東", "的淨利潤"]],
        "cashFlow": [["经营活动产生的现金", "流量净额"], ["經營活動產生的現金", "流量淨額"]],
    }

    FINANCIAL_DATA_HINTS = ["财务数据", "财务指标", "全年经营成果", "財務數據", "財務指標", "全年經營成果"]

    PATTERNS = {
        "revenue": [r"[营业營業](?:总|總)?收入[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|萬億|亿元|億元|万元|萬元|元)?"],
        "profit": [r"[归歸]属?于(?:上市公司|母公司)股东的[净淨]利润[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|萬億|亿元|億元|万元|萬元|元)?", r"(?:归母|歸母)?[净淨]利润[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|萬億|亿元|億元|万元|萬元|元)?"],
        "totalAssets": [r"(?:总资产|總資產|资产总计|資產總計)[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|萬億|亿元|億元|万元|萬元|元)?"],
        "totalLiabilities": [r"(?:总负债|總負債|负债合计|負債合計)[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|萬億|亿元|億元|万元|萬元|元)?"],
        "cashFlow": [r"[经营經營]活动产生的现金流量[净淨]额[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|萬億|亿元|億元|万元|萬元|元)?", r"[经营經營].*现金流[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|萬億|亿元|億元|万元|萬元|元)?"],
        "grossMargin": [r"毛利率[：:\s\n]*([\d,]+\.?\d*)\s*%"],
    }

    def extract(self, text: str) -> dict:
        results = {}
        statement_results = self._extract_from_statement_tables(text)
        results.update(statement_results)
        for key, patterns in self.PATTERNS.items():
            if key in results:
                continue
            for pattern in patterns:
                match = re.search(pattern, text)
                if not match:
                    continue
                value = float(match.group(1).replace(",", ""))
                unit = "%" if key == "grossMargin" else "亿"
                raw_unit = match.group(2) if len(match.groups()) > 1 else None
                if raw_unit in {"万元", "萬元"}:
                    value = value / 10000
                elif raw_unit in {"万亿", "萬億"}:
                    value = value * 10000
                elif raw_unit == "元" or (raw_unit is None and key != "grossMargin" and value > 1000000):
                    value = value / 100000000
                elif raw_unit is None and key != "grossMargin" and value > 10000:
                    value = value / 100
                if key != "grossMargin" and value <= 0.0001:
                    continue
                results[key] = {
                    "value": value,
                    "unit": unit,
                    "confidence": 0.95,
                    "method": "regex",
                    "matchedText": match.group(0),
                }
                break
        return results

    def _extract_from_statement_tables(self, text: str) -> dict:
        results = {}
        for key, aliases in self.INDICATOR_ALIASES.items():
            match = self._find_statement_value(text, key, aliases)
            if not match:
                continue
            value, matched_text = match
            results[key] = {
                "value": value,
                "unit": "亿",
                "confidence": 0.9,
                "method": "statement_table",
                "matchedText": matched_text,
            }
        return results

    def _find_statement_value(self, text: str, key: str, aliases: list[str]) -> tuple[float, str] | None:
        best = None
        for page_text in self._split_pages(text):
            lines = [line.strip() for line in page_text.splitlines() if line.strip()]
            page_score = self._statement_page_score(page_text, key)
            if page_score <= 0:
                continue
            for alias in aliases:
                for index in range(len(lines)):
                    matched, alias_line_count = self._match_alias_block(lines, index, alias)
                    if not matched:
                        continue
                    block_lines = lines[index:index + max(alias_line_count + 4, 5)]
                    block = " ".join(block_lines)
                    if self._is_excluded_candidate(key, block):
                        continue
                    value = self._extract_first_main_value(block_lines, alias_line_count)
                    if value is None:
                        continue
                    if value < 1000 and key != "grossMargin":
                        continue
                    value = self._normalize_statement_value(value, page_text)
                    if value <= 0:
                        continue
                    candidate = (page_score, value, block)
                    if best is None or candidate[0] > best[0]:
                        best = candidate
            for alias_group in self.MULTILINE_ALIASES.get(key, []):
                for index in range(len(lines)):
                    matched, alias_line_count = self._match_multiline_alias(lines, index, alias_group)
                    if not matched:
                        continue
                    block_lines = lines[index:index + max(alias_line_count + 4, 5)]
                    block = " ".join(block_lines)
                    if self._is_excluded_candidate(key, block):
                        continue
                    value = self._extract_first_main_value(block_lines, alias_line_count)
                    if value is None:
                        continue
                    if value < 1000 and key != "grossMargin":
                        continue
                    value = self._normalize_statement_value(value, page_text)
                    if value <= 0:
                        continue
                    candidate = (page_score + 2, value, block)
                    if best is None or candidate[0] > best[0]:
                        best = candidate
        if best is None:
            return None
        return best[1], best[2]

    def _split_pages(self, text: str) -> list[str]:
        parts = re.split(r"--- 第 \d+ 页 ---\n", text)
        return [part for part in parts if part.strip()]

    def _statement_page_score(self, page_text: str, key: str) -> int:
        score = 0
        if "主要会计数据和财务指标" in page_text:
            score += 15
        if any(hint in page_text for hint in self.FINANCIAL_DATA_HINTS):
            score += 18
        if "分季度财务数据" in page_text:
            score -= 12
        if key in {"revenue", "profit", "cashFlow"} and "合并利润表" in page_text:
            score += 12
        if key in {"totalAssets", "totalLiabilities"} and "合并资产负债表" in page_text:
            score += 14
        if key in {"revenue", "profit", "cashFlow", "totalAssets", "totalLiabilities"} and "人民币百万元" in page_text:
            score += 10
        if key in {"totalAssets", "totalLiabilities"} and "资产总计" in page_text and "所有者权益" in page_text:
            score += 12
        if key == "totalLiabilities" and "流动负债" in page_text and "非流动负债" in page_text and "负债合计" in page_text:
            score += 10
        if key == "totalLiabilities" and "负债合计" in page_text and "所有者权益" in page_text:
            score += 10
        if key == "cashFlow" and ("现金流量表" in page_text or "5、现金流" in page_text):
            score += 12
        if "项目" in page_text and "2025 年" in page_text:
            score += 6
        if "单位：千元" in page_text or "单位:千元" in page_text or "单位：万元" in page_text or "单位:万元" in page_text or "单位：元" in page_text or "单位:元" in page_text:
            score += 3
        if "利润分配预案" in page_text or "每10股派发" in page_text or "应付股利" in page_text:
            score -= 10
        return score

    def _extract_first_main_value(self, block: str, alias: str) -> float | None:
        numbers = re.findall(r"-?\d[\d,]*(?:\.\d+)?", block)
        for token in numbers:
            if re.fullmatch(r"20\d{2}", token):
                continue
            return float(token.replace(',', ''))
        return None

    def _match_alias_block(self, lines: list[str], index: int, alias: str) -> tuple[bool, int]:
        alias_compact = re.sub(r"\s+", "", alias)
        for span in range(1, 4):
            if index + span > len(lines):
                break
            joined = re.sub(r"\s+", "", "".join(lines[index:index + span]))
            if joined.startswith(alias_compact):
                if len(joined) > len(alias_compact):
                    next_char = joined[len(alias_compact)]
                    if '\u4e00' <= next_char <= '\u9fff':
                        continue
                return True, span
        return False, 0

    def _match_multiline_alias(self, lines: list[str], index: int, alias_group: list[str]) -> tuple[bool, int]:
        if index + len(alias_group) > len(lines):
            return False, 0
        for offset, alias_part in enumerate(alias_group):
            if alias_part not in lines[index + offset]:
                return False, 0
        return True, len(alias_group)

    def _extract_first_main_value(self, block_lines: list[str], alias_line_count: int) -> float | None:
        relevant_lines = block_lines[alias_line_count:]
        for line in relevant_lines:
            if '%' in line and not re.search(r"-?\d[\d,]*(?:\.\d+)?\s*$", line):
                continue
            numbers = re.findall(r"-?\d[\d,]*(?:\.\d+)?", line)
            for token in numbers:
                if re.fullmatch(r"20\d{2}", token):
                    continue
                if len(token.replace(',', '')) <= 2:
                    continue
                return float(token.replace(',', ''))
        return None

    def _looks_like_financial_statement(self, context: str) -> bool:
        keywords = ["合并资产负债表", "合并利润表", "合并现金流量表", "母公司", "单位：元", "单位:元", "单位：万元", "单位:万元"]
        return any(keyword in context for keyword in keywords)

    def _normalize_statement_value(self, value: float, context: str) -> float:
        compact_context = re.sub(r"\s+", "", context)
        if "万亿" in compact_context or "萬億" in compact_context:
            return value * 10000
        if "百万元" in compact_context or "百萬元" in compact_context:
            return value / 100
        if "千元" in compact_context:
            return value / 100000
        if "万元" in compact_context or "萬元" in compact_context:
            return value / 10000
        if "单位：元" in compact_context or "單位：元" in compact_context or "单位:元" in compact_context or "單位:元" in compact_context or "（元）" in compact_context:
            return value / 100000000
        # Statement continuation pages often omit the unit; for A-share annual reports
        # large table values almost always continue the prior page's "万元" convention.
        if value > 1000000:
            return value / 10000
        if value > 10000:
            return value / 10000
        return value

    def _is_excluded_candidate(self, key: str, block: str) -> bool:
        compact = re.sub(r"\s+", "", block)
        if any(marker in compact for marker in ["合并财务报表", "财务报表附注", "重要会计政策", "直接相关项目金额", "子公司指由本集团控制"]):
            return True
        if key == "profit" and (compact.startswith("扣除非经常性损益") or "扣除非经常性损益后的净利润" in compact or "扣除非經常性損益後的淨利潤" in compact):
            return True
        if key == "totalLiabilities" and (compact.startswith("流动负债合计") or compact.startswith("非流动负债合计") or compact.startswith("流動負債合計") or compact.startswith("非流動負債合計")):
            return True
        if key == "totalLiabilities" and "所有者权益" in compact[:40]:
            return True
        return False


regex_extractor = RegexExtractor()
