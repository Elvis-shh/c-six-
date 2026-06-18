import re


class RegexExtractor:
    PATTERNS = {
        "revenue": [r"营业(?:总)?收入[：:\s]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "profit": [r"(?:归母)?净利润[：:\s]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "totalAssets": [r"(?:总资产|资产总计)[：:\s]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "totalLiabilities": [r"(?:总负债|负债合计)[：:\s]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "cashFlow": [r"经营.*现金流[：:\s]*([\d,]+\.?\d*)\s*(万亿|亿元|万元|元)?"],
        "grossMargin": [r"毛利率[：:\s]*([\d,]+\.?\d*)\s*%"],
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
