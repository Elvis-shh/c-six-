import httpx
import xlrd


class IndexService:
    EASTMONEY_LIST_URL = "https://push2.eastmoney.com/api/qt/clist/get"
    CSI300_CONS_URL = "https://oss-ch.csindex.com.cn/static/html/csindex/public/uploads/file/autofile/cons/000300cons.xls"

    async def get_csi300_constituents(self) -> list[dict]:
        try:
            return await self._get_from_csindex_file()
        except Exception:
            return await self._get_from_eastmoney()

    async def _get_from_csindex_file(self) -> list[dict]:
        headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/125 Safari/537.36"}
        async with httpx.AsyncClient(timeout=60, headers=headers, follow_redirects=True) as client:
            response = await client.get(self.CSI300_CONS_URL)
            response.raise_for_status()

        workbook = xlrd.open_workbook(file_contents=response.content)
        sheet = workbook.sheet_by_index(0)
        header_row = [str(sheet.cell_value(0, col)).strip() for col in range(sheet.ncols)]
        code_col = self._find_column(header_row, ["成分券代码", "证券代码", "Constituent Code"])
        name_col = self._find_column(header_row, ["成分券名称", "证券简称", "Constituent Name"])
        market_col = self._find_column(header_row, ["交易所", "Exchange"])

        rows = []
        for row_index in range(1, sheet.nrows):
            raw_code = str(sheet.cell_value(row_index, code_col)).strip()
            code = raw_code.split(".")[0].zfill(6)
            name = str(sheet.cell_value(row_index, name_col)).strip()
            market_text = str(sheet.cell_value(row_index, market_col)).strip() if market_col >= 0 else ""
            if not code or code == "000000":
                continue
            rows.append({
                "code": code,
                "name": name,
                "market": self._market_from_text(code, market_text),
                "source": "csindex:000300cons.xls",
            })
        if len(rows) < 250:
            raise ValueError(f"中证指数成分股文件返回数量异常: {len(rows)}")
        return rows[:300]

    async def _get_from_eastmoney(self) -> list[dict]:
        params = {
            "pn": 1,
            "pz": 500,
            "po": 1,
            "np": 1,
            "fltt": 2,
            "invt": 2,
            "fid": "f3",
            "fs": "b:BK0500",
            "fields": "f12,f14,f13,f100",
        }
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/125 Safari/537.36",
            "Referer": "https://quote.eastmoney.com/center/boardlist.html",
        }
        async with httpx.AsyncClient(timeout=30, headers=headers, follow_redirects=True) as client:
            response = await client.get(self.EASTMONEY_LIST_URL, params=params)
            response.raise_for_status()
            payload = response.json()

        rows = (payload.get("data") or {}).get("diff") or []
        constituents = []
        for row in rows:
            code = str(row.get("f12") or "").zfill(6)
            name = row.get("f14") or code
            if not code or code == "000000":
                continue
            constituents.append({
                "code": code,
                "name": name,
                "market": self._market(code),
                "source": "eastmoney:BK0500",
            })
        if len(constituents) < 250:
            raise ValueError(f"沪深300成分股返回数量异常: {len(constituents)}")
        return constituents[:300]

    def _find_column(self, headers: list[str], candidates: list[str]) -> int:
        for candidate in candidates:
            for index, header in enumerate(headers):
                if candidate in header:
                    return index
        raise ValueError(f"未找到列: {candidates}")

    def _market_from_text(self, code: str, market_text: str) -> str:
        if "上海" in market_text or "SH" in market_text.upper():
            return "SH"
        if "深圳" in market_text or "SZ" in market_text.upper():
            return "SZ"
        return self._market(code)

    def _market(self, code: str) -> str:
        if code.startswith("6"):
            return "SH"
        if code.startswith(("0", "2", "3")):
            return "SZ"
        return "SZ"


index_service = IndexService()
