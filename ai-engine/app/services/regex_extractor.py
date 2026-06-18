import re


class RegexExtractor:
    PATTERNS = {
        "revenue": [r"营业(?:总)?收入[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "profit": [r"归属于上市公司股东的净利润[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?", r"(?:归母)?净利润[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "totalAssets": [r"(?:总资产|资产总计)[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "totalLiabilities": [r"(?:总负债|负债合计)[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "cashFlow": [r"经营活动产生的现金流量净额[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?", r"经营.*现金流[：:\s\n]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "grossMargin": [r"毛利率[：:\s\n]*([\d,]+\.?\d*)\s*%"],
    }

    def extract(self, text: str) -> dict:
        results = {}
        for key, patterns in self.PATTERNS.items():
            for pattern in patterns:
                match = re.search(pattern, text)
                if not match:
                    continue
                value = float(match.group(1).replace(",", ""))
                unit = "%" if key == "grossMargin" else "亿"
                raw_unit = match.group(2) if len(match.groups()) > 1 else None
                if raw_unit == "万元":
                    value = value / 10000
                elif raw_unit == "万亿":
                    value = value * 10000
                elif raw_unit == "元" or (raw_unit is None and key != "grossMargin" and value > 1000000):
                    value = value / 100000000
                results[key] = {
                    "value": value,
                    "unit": unit,
                    "confidence": 0.95,
                    "method": "regex",
                    "matchedText": match.group(0),
                }
                break
        return results


regex_extractor = RegexExtractor()
