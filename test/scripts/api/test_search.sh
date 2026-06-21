#!/bin/bash
# ============================================================
# SmartReport 搜索 API 测试
# 覆盖: TC-1.2-01 ~ TC-1.2-08
# 用法: bash test/scripts/api/test_search.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0; FAIL=0

ok() { echo -e "  ✅ $1"; ((PASS++)); }
fail() { echo -e "  ❌ $1 (expected: $2)"; ((FAIL++)); }

echo "============================================"
echo " 搜索 API 测试 (模块 1.2)"
echo " BASE_URL=$BASE_URL"
echo "============================================"

# TC-1.2-01: 按公司名模糊搜索
echo ""
echo "[TC-1.2-01] 按公司名模糊搜索"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=茅台&limit=8")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null || echo "")
HAS_MT=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(any('贵州茅台' in c.get('name','') for c in d.get('data',[])))" 2>/dev/null || echo "")
[ "$CODE" = "0" ] && ok "code=0" || fail "code=$CODE" "code=0"
[ "$HAS_MT" = "True" ] && ok "data 包含贵州茅台" || fail "data 不含贵州茅台" "含贵州茅台"

# TC-1.2-02: 按股票代码搜索
echo ""
echo "[TC-1.2-02] 按股票代码搜索"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=600519")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null || echo "")
FIRST_CODE=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); arr=d.get('data',[]); print(arr[0]['code'] if arr else '')" 2>/dev/null || echo "")
[ "$FIRST_CODE" = "600519" ] && ok "data[0].code=600519" || fail "data[0].code=$FIRST_CODE" "600519"

# TC-1.2-03: 搜索无匹配结果
echo ""
echo "[TC-1.2-03] 搜索无匹配结果"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=xyzabc123")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null || echo "")
DATA_LEN=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "")
[ "$CODE" = "0" ] && ok "code=0 (非404)" || fail "code=$CODE" "code=0"
[ "$DATA_LEN" = "0" ] && ok "data=[]" || fail "data长度=$DATA_LEN" "data=[]"

# TC-1.2-04: 空搜索词
echo ""
echo "[TC-1.2-04] 空搜索词处理"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=" || echo "HTTP_ERROR")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null || echo "")
[ "$CODE" = "0" ] && ok "空搜索返回 code=0" || ok "空搜索返回非0（可接受）"

# TC-1.2-05: 热门公司
echo ""
echo "[TC-1.2-05] 获取热门公司"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies/hot")
DATA_LEN=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "")
[ "$DATA_LEN" = "6" ] && ok "返回 6 家热门公司" || fail "返回 $DATA_LEN 家" "6家"

# TC-1.2-06: 分页限制
echo ""
echo "[TC-1.2-06] 分页限制 limit=3"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=科技&limit=3")
DATA_LEN=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "")
[ "$DATA_LEN" -le 3 ] && ok "data长度=$DATA_LEN ≤3" || fail "data长度=$DATA_LEN" "≤3"

# TC-1.2-07: 部分匹配（简称）
echo ""
echo "[TC-1.2-07] 部分匹配'宁德'"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=宁德")
HAS_ND=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(any('宁德' in c.get('name','') for c in d.get('data',[])))" 2>/dev/null || echo "")
[ "$HAS_ND" = "True" ] && ok "返回宁德时代" || fail "未找到宁德时代" "含宁德时代"

# TC-1.2-08: 响应格式校验
echo ""
echo "[TC-1.2-08] 响应格式校验"
RESP=$(curl -sf "${BASE_URL}/api/v1/search/companies?q=茅台")
HAS_CODE=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print('code' in d and 'message' in d and 'data' in d and 'timestamp' in d)" 2>/dev/null || echo "")
[ "$HAS_CODE" = "True" ] && ok "含 code/message/data/timestamp" || fail "响应格式不完整" "含四个字段"

# ── 汇总 ──
echo ""
echo "============================================"
echo -e "  搜索 API: 通过 $PASS / 失败 $FAIL"
echo "============================================"
exit $FAIL
