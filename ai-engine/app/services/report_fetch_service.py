from pathlib import Path
from urllib.parse import urljoin

import httpx
import fitz

from app.services.ner_service import ner_service


class ReportFetchService:
    CNINFO_QUERY_URL = "https://www.cninfo.com.cn/new/hisAnnouncement/query"
    CNINFO_STATIC_URL = "https://static.cninfo.com.cn/"

    def __init__(self, data_dir: str = "/app/data/reports"):
        self.data_dir = Path(data_dir)
        self.data_dir.mkdir(parents=True, exist_ok=True)

    REPORT_TYPES = {
        "annual": {
            "category": "category_ndbg_szsh",
            "label": "年报",
            "date_month_day": "01-01~12-31",
            "title_keywords": ["年度报告", "年报"],
            "exclude": ["英文", "取消"],
        },
        "quarter1": {
            "category": "category_yjdbg_szsh",
            "label": "一季报",
            "date_month_day": "03-01~06-30",
            "title_keywords": ["第一季度报告", "一季度报告"],
            "exclude": ["正文", "英文", "取消"],
        },
        "semi_annual": {
            "category": "category_bndbg_szsh",
            "label": "半年报",
            "date_month_day": "06-01~09-30",
            "title_keywords": ["半年度报告", "半年报"],
            "exclude": ["摘要", "英文", "取消"],
        },
        "quarter3": {
            "category": "category_sjdbg_szsh",
            "label": "三季报",
            "date_month_day": "09-01~12-31",
            "title_keywords": ["第三季度报告", "三季度报告"],
            "exclude": ["正文", "英文", "取消"],
        },
    }

    SEARCH_ALIASES = {
        "601211": ["国泰君安", "国泰海通"],
    }

    async def fetch_report(self, company_code: str, year: int, report_type: str = "annual") -> dict:
        report_config = self.REPORT_TYPES.get(report_type)
        if not report_config:
            raise ValueError(f"不支持的报告类型: {report_type}")

        announcement = await self._find_report(company_code, year, report_type)
        if not announcement:
            raise ValueError(f"未找到 {company_code} {year} {report_config['label']}公告")

        pdf_url = urljoin(self.CNINFO_STATIC_URL, announcement["adjunctUrl"])
        file_name = self._safe_file_name(announcement.get("announcementTitle") or f"{company_code}-{year}年报") + ".pdf"
        target_dir = self.data_dir / company_code
        target_dir.mkdir(parents=True, exist_ok=True)
        file_path = target_dir / file_name

        headers = self._headers()
        async with httpx.AsyncClient(timeout=60, follow_redirects=True, headers=headers) as client:
            response = await client.get(pdf_url)
            response.raise_for_status()
            file_path.write_bytes(response.content)

        return {
            "companyCode": company_code,
            "reportYear": year,
            "reportType": report_type,
            "title": announcement.get("announcementTitle"),
            "publishedAt": self._format_date(announcement.get("announcementTime")),
            "sourceUrl": pdf_url,
            "fileName": file_name,
            "filePath": str(file_path),
            "fileType": "pdf",
            "fileSize": file_path.stat().st_size,
        }

    async def fetch_annual_report(self, company_code: str, year: int) -> dict:
        return await self.fetch_report(company_code, year, "annual")

    async def parse_report_file(self, file_path: str) -> dict:
        path = Path(file_path)
        if not path.exists() or not path.is_file():
            raise ValueError(f"财报文件不存在: {file_path}")

        text_parts = []
        with fitz.open(path) as doc:
            for page_index, page in enumerate(doc):
                text_parts.append(f"--- 第 {page_index + 1} 页 ---\n{page.get_text()}")

        text = "\n".join(text_parts)
        extracted = await ner_service.extract(text)
        return {
            "filePath": str(path),
            "pageCount": len(text_parts),
            "textLength": len(text),
            "extractedData": extracted,
        }

    async def extract_quote_chunks(self, file_path: str) -> list[dict]:
        path = Path(file_path)
        if not path.exists() or not path.is_file():
            raise ValueError(f"财报文件不存在: {file_path}")

        chunks = []
        with fitz.open(path) as doc:
            for page_index, page in enumerate(doc):
                text = self._clean_page_text(page.get_text())
                if len(text) < 80:
                    continue
                chunks.append({
                    "page": page_index + 1,
                    "source": path.name,
                    "content": text[:4000],
                })
        if not chunks:
            raise ValueError(f"财报文件无可用正文: {file_path}")
        return chunks

    async def _find_report(self, company_code: str, year: int, report_type: str) -> dict | None:
        market = self._market(company_code)
        report_config = self.REPORT_TYPES[report_type]
        start_md, end_md = report_config["date_month_day"].split("~")
        publish_year = year + 1 if report_type == "annual" else year
        payload = {
            "pageNum": 1,
            "pageSize": 30,
            "column": market["column"],
            "tabName": "fulltext",
            "plate": market["plate"],
            "stock": "",
            "searchkey": company_code,
            "secid": "",
            "category": report_config["category"],
            "trade": "",
            "seDate": f"{publish_year}-{start_md}~{publish_year}-{end_md}",
            "sortName": "",
            "sortType": "",
            "isHLtitle": "true",
        }

        announcements = await self._query_candidate_announcements(payload, company_code)
        candidates = [item for item in announcements if self._is_target_report(item, year, report_type)]
        if not candidates:
            return None
        return sorted(candidates, key=self._candidate_sort_key, reverse=True)[0]

    async def _query_candidate_announcements(self, payload: dict, company_code: str) -> list[dict]:
        attempts = []
        attempts.append({**payload})
        attempts.append({**payload, "column": "szse", "plate": "sz", "searchkey": company_code})
        attempts.append({**payload, "searchkey": "", "stock": f"{company_code},{self._org_id_guess(company_code)}"})
        for alias in self.SEARCH_ALIASES.get(company_code, []):
            attempts.append({**payload, "searchkey": alias, "stock": ""})

        seen = set()
        announcements = []
        for attempt in attempts:
            data = await self._query_announcements(attempt)
            for item in data.get("announcements") or []:
                announcement_id = item.get("announcementId") or item.get("adjunctUrl")
                if announcement_id in seen:
                    continue
                seen.add(announcement_id)
                announcements.append(item)
        return announcements

    async def _query_announcements(self, payload: dict) -> dict:
        async with httpx.AsyncClient(timeout=30, headers=self._headers()) as client:
            response = await client.post(self.CNINFO_QUERY_URL, data=payload)
            response.raise_for_status()
            return response.json()

    def _is_target_report(self, item: dict, year: int, report_type: str) -> bool:
        title = item.get("announcementTitle") or ""
        report_config = self.REPORT_TYPES[report_type]
        if any(word in title for word in report_config["exclude"]):
            return False
        return any(
            f"{year}{keyword}" in title or f"{year}年{keyword}" in title or f"{year} 年{keyword}" in title
            for keyword in report_config["title_keywords"]
        )

    def _candidate_sort_key(self, item: dict) -> tuple:
        title = item.get("announcementTitle") or ""
        return (
            0 if "摘要" in title else 1,
            item.get("announcementTime") or 0,
        )

    def _market(self, company_code: str) -> dict:
        if company_code.startswith("6"):
            return {"column": "sse", "plate": "sh"}
        if company_code.startswith(("0", "2", "3")):
            return {"column": "szse", "plate": "sz"}
        if company_code.startswith(("4", "8")):
            return {"column": "neeq", "plate": "bj"}
        return {"column": "szse", "plate": "sz"}

    def _org_id_guess(self, company_code: str) -> str:
        if company_code.startswith("6"):
            return f"gssh{company_code.zfill(7)}"
        return f"gssz{company_code.zfill(7)}"

    def _headers(self) -> dict:
        return {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/125 Safari/537.36",
            "Referer": "https://www.cninfo.com.cn/new/commonUrl/pageOfSearch?url=disclosure/list/search",
            "Accept": "application/json, text/plain, */*",
        }

    def _safe_file_name(self, name: str) -> str:
        forbidden = '<>:"/\\|?*'
        safe = "".join("_" if char in forbidden else char for char in name)
        return safe[:180]

    def _clean_page_text(self, text: str) -> str:
        return " ".join((text or "").split()).strip()

    def _format_date(self, timestamp: int | None) -> str | None:
        if not timestamp:
            return None
        from datetime import datetime
        return datetime.fromtimestamp(timestamp / 1000).date().isoformat()


report_fetch_service = ReportFetchService()
