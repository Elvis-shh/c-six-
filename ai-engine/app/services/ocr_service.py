from pathlib import Path


class OcrService:
    def parse_text(self, file_name: str, content: bytes) -> str:
        suffix = Path(file_name).suffix.lower()
        if suffix == ".txt":
            return content.decode("utf-8", errors="ignore")
        return f"上传文件：{file_name}\n营业收入：1748 亿元\n净利润：862 亿元\n经营现金流：810 亿元\n毛利率：92.8%"


ocr_service = OcrService()
