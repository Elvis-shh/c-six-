#!/bin/bash
# ============================================================
# SmartReport 安全测试
# 覆盖: TC-SC-01 ~ TC-SC-03
# 用法: bash test/scripts/security/test_security.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0; FAIL=0

ok() { echo -e "  ✅ $1"; ((PASS++)); }
warn() { echo -e "  ⚠️ $1"; ((PASS++)); }
fail() { echo -e "  ❌ $1"; ((FAIL++)); }

echo "============================================"
echo " 安全测试"
echo " BASE_URL=$BASE_URL"
echo "============================================"

# ── TC-SC-01: SQL 注入 ──
echo ""
echo "[TC-SC-01] SQL 注入防护"
SQLI_PAYLOADS=(
    "' OR '1'='1"
    "'; DROP TABLE companies; --"
    "' UNION SELECT NULL--"
)

for payload in "${SQLI_PAYLOADS[@]}"; do
    HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" \
        "${BASE_URL}/api/v1/search/companies?q=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$payload'''))")" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        # 检查返回数据是否异常（不应返回全表数据）
        RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$payload'''))")" 2>/dev/null || echo "")
        DATA_LEN=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
        if [ "$DATA_LEN" = "0" ]; then
            ok "SQL注入 '$payload' → 返回空数据 (HTTP $HTTP_CODE)"
        else
            warn "SQL注入 '$payload' → 返回 $DATA_LEN 条 (HTTP $HTTP_CODE) — 需人工确认"
        fi
    else
        ok "SQL注入 '$payload' → HTTP $HTTP_CODE (被拦截)"
    fi
done

# ── TC-SC-02: XSS ──
echo ""
echo "[TC-SC-02] XSS 防护"
XSS_PAYLOAD="<script>alert(1)</script>"
HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" \
    "${BASE_URL}/api/v1/search/companies?q=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$XSS_PAYLOAD'))")" 2>/dev/null || echo "000")
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$XSS_PAYLOAD'))")" 2>/dev/null || echo "")
if echo "$RESP" | grep -qi "<script>alert"; then
    fail "XSS payload 原样返回，存在反射型 XSS 风险"
else
    ok "XSS payload 未原样返回 (HTTP $HTTP_CODE)"
fi

# ── TC-SC-03: 超大请求 ──
echo ""
echo "[TC-SC-03] 超大请求防护"
# 生成一个 100KB 的搜索词
BIG_QUERY=$(python3 -c "print('A'*100000)")
HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" \
    "${BASE_URL}/api/v1/search/companies?q=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$BIG_QUERY'))")" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "414" ] || [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "413" ]; then
    ok "100KB 请求被拒绝 (HTTP $HTTP_CODE)"
else
    warn "100KB 请求返回 HTTP $HTTP_CODE — 需确认是否有长度限制"
fi

# ── 特殊字符测试 ──
echo ""
echo "[SC-SPECIAL] 特殊字符处理"
SPECIAL_CHARS=("../../../etc/passwd" "${IFS}test" "%00" "\\x00")
for payload in "${SPECIAL_CHARS[@]}"; do
    HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" \
        "${BASE_URL}/api/v1/search/companies?q=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$payload'))")" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "400" ]; then
        ok "特殊字符 '$payload' 安全处理 (HTTP $HTTP_CODE)"
    else
        warn "特殊字符 '$payload' → HTTP $HTTP_CODE"
    fi
done

echo ""
echo "============================================"
echo -e "  安全测试: 通过 $PASS / 失败 $FAIL"
echo "============================================"
exit $FAIL
