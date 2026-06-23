import re
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
        "601857": ["中国石油", "中国石油天然气股份有限公司"],
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

        # Extract company code and year from first page
        first_page = text_parts[0] if text_parts else ""
        company_code = self._extract_company_code(first_page)
        company_name = self._extract_company_name(first_page)
        report_year = self._extract_report_year(first_page)

        # Detect industry from first few pages
        head_text = "\n".join(text_parts[:6]) if len(text_parts) > 1 else text
        industry = self._detect_industry(head_text)

        return {
            "filePath": str(path),
            "pageCount": len(text_parts),
            "textLength": len(text),
            "extractedData": extracted,
            "companyCode": company_code,
            "companyName": company_name,
            "reportYear": report_year,
            "industry": industry,
        }

    def _extract_company_code(self, text: str) -> str | None:
        m = re.search(r"公司代码[：:]\s*(\d{6})", text)
        if m:
            return m.group(1)
        m = re.search(r"股票代码[：:]\s*(\d{6})", text)
        return m.group(1) if m else None

    def _extract_company_name(self, text: str) -> str | None:
        m = re.search(r"公司简称[：:]\s*(\S+)", text)
        return m.group(1) if m else None

    def _extract_report_year(self, text: str) -> int | None:
        m = re.search(r"(\d{4})\s*年[度\s]*(?:年度)?报告", text)
        if m:
            return int(m.group(1))
        m = re.search(r"(\d{4})\s*年[度\s]*年报", text)
        return int(m.group(1)) if m else None

    def _detect_industry(self, text: str) -> str:
        """Detect industry from company description in annual report."""
        rules = [
            (["银行", "存款", "贷款", "利息收入", "净息差"], "银行"),
            (["证券", "券商", "投行", "经纪业务", "自营业务"], "证券"),
            (["保险", "保费", "赔付", "再保险", "精算"], "保险"),
            (["白酒", "酿酒", "酒类", "基酒", "勾调"], "白酒"),
            (["水泥", "熟料", "混凝土", "骨料"], "建材"),
            (["煤炭", "煤矿", "采掘", "煤化工", "洗煤"], "煤炭"),
            (["石油", "油田", "炼化", "成品油", "勘探开发"], "石油石化"),
            (["电力", "发电", "电网", "水电", "火电", "核电", "风电", "光伏", "新能源"], "电力"),
            (["汽车", "整车", "乘用车", "商用车", "新能源车"], "汽车"),
            (["芯片", "半导体", "晶圆", "集成电路", "封装测试"], "半导体"),
            (["医药", "制药", "药品", "制剂", "临床", "新药", "仿制药", "CXO", "生物药"], "医药"),
            (["医疗器械", "影像设备", "体外诊断", "植入"], "医疗器械"),
            (["钢铁", "钢材", "粗钢", "热轧", "冷轧"], "钢铁"),
            (["有色金属", "铜矿", "铝业", "黄金", "稀土", "锂业", "钴业"], "有色金属"),
            (["房地产", "地产", "物业", "楼盘", "交付面积"], "房地产"),
            (["建筑", "施工", "基建", "工程承包", "总承包"], "建筑"),
            (["航空", "机场", "航司", "旅客运输量", "客座率"], "航空"),
            (["铁路", "高铁", "轨道交通", "动车组"], "交通运输"),
            (["港口", "航运", "集装箱", "吞吐量"], "交通运输"),
            (["食品", "饮料", "乳业", "肉制品", "调味品", "食用油"], "食品饮料"),
            (["家电", "空调", "冰箱", "洗衣机", "小家电"], "家电"),
            (["化工", "化学原料", "精细化工", "化肥", "农药"], "化工"),
            (["通信", "电信", "5G", "基站", "光纤", "移动通信"], "通信"),
            (["软件", "IT服务", "云计算", "人工智能", "大数据"], "信息技术"),
            (["消费电子", "手机", "可穿戴", "智能终端"], "消费电子"),
            (["零售", "超市", "百货", "连锁", "电商"], "商贸零售"),
        ]
        for keywords, industry in rules:
            score = sum(1 for kw in keywords if kw in text)
            if score >= 2:
                return industry
        return "其他行业"

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
            f"{year}{keyword}" in title
            or f"{year}年{keyword}" in title
            or f"{year} 年{keyword}" in title
            or f"{year}年度报告" in title
            or f"{year} 年度报告" in title
            or f"{year}半年度报告" in title
            or f"{year} 半年度报告" in title
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
