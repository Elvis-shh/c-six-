from app.services.regex_extractor import regex_extractor


class NerService:
    async def extract(self, text: str) -> dict:
        data = regex_extractor.extract(text)
        for key in ["revenue", "profit", "cashFlow", "grossMargin"]:
            data.setdefault(key, {
                "value": None,
                "unit": "%" if key == "grossMargin" else "亿",
                "confidence": 0,
                "method": "none",
                "matchedText": "",
            })
        return data


ner_service = NerService()
