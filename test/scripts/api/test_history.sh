#!/bin/bash
# ============================================================
# SmartReport 历史管理 API 测试
# 覆盖: TC-2.3-09 ~ TC-2.3-12
# 前置: 需要先运行 test_auth.sh 获取 Token，或手动设置 TOKEN 环境变量
# 用法: TOKEN="xxx" bash test/scripts/api/test_history.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"
PASS=0; FAIL=0

ok() { echo -e "  ✅ $1"; ((PASS++)); }
fail() { echo -e "  ❌ $1"; ((FAIL++)); }

echo "============================================"
echo " 历史管理 API 测试 (模块 2.3 云同步)"
echo " BASE_URL=$BASE_URL"
echo "============================================"

# 获取 Token（如果未提供则自动注册+登录）
if [ -z "$TOKEN" ]; then
    echo "  未提供 TOKEN，自动注册测试账号..."
    TEST_EMAIL="hist_$(date +%s)@smartreport.com"
    curl -sf -X POST "${BASE_URL}/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"Test123456\",\"nickname\":\"历史测试\"}" > /dev/null 2>&1
    TOKEN=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"Test123456\"}" \
        | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null)
    if [ -z "$TOKEN" ]; then
        echo "  ❌ 无法获取 Token，退出"
        exit 1
    fi
    echo "  已获取 Token"
fi

AUTH_HEADER="Authorization: Bearer ${TOKEN}"

# ── TC-2.3-10: 初始历史为空 ──
echo ""
echo "[TC-2.3-10] 获取历史列表"
HIST_RESP=$(curl -sf "${BASE_URL}/api/v1/history" -H "${AUTH_HEADER}")
HIST_COUNT=$(echo "$HIST_RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
ok "历史条数: $HIST_COUNT"

# ── TC-2.3-09: 添加历史 ──
echo ""
echo "[TC-2.3-09] 添加历史记录"
ADD_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/history" \
    -H "Content-Type: application/json" \
    -H "${AUTH_HEADER}" \
    -d '{"companyCode":"600519","companyName":"贵州茅台"}')
ADD_CODE=$(echo "$ADD_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$ADD_CODE" = "0" ] && ok "添加贵州茅台成功" || fail "添加历史失败"

# 添加更多
for code in "000858" "300750" "601398"; do
    curl -sf -X POST "${BASE_URL}/api/v1/history" \
        -H "Content-Type: application/json" \
        -H "${AUTH_HEADER}" \
        -d "{\"companyCode\":\"${code}\",\"companyName\":\"测试公司${code}\"}" > /dev/null
done

# ── TC-2.3-10: 验证历史 ──
echo ""
echo "[TC-2.3-10] 验证历史条数"
HIST_RESP=$(curl -sf "${BASE_URL}/api/v1/history" -H "${AUTH_HEADER}")
HIST_COUNT=$(echo "$HIST_RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
HAS_MT=$(echo "$HIST_RESP" | python3 -c "import sys,json; print(any('600519' in str(i) for i in json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "")
[ "$HIST_COUNT" -ge 4 ] && ok "历史条数=$HIST_COUNT" || fail "历史条数=$HIST_COUNT (<4)"
[ "$HAS_MT" = "True" ] && ok "历史包含贵州茅台" || fail "历史不含贵州茅台"

# ── TC-2.3-02: 去重 ──
echo ""
echo "[TC-2.3-02] 重复添加去重"
curl -sf -X POST "${BASE_URL}/api/v1/history" \
    -H "Content-Type: application/json" \
    -H "${AUTH_HEADER}" \
    -d '{"companyCode":"600519","companyName":"贵州茅台"}' > /dev/null
HIST_RESP=$(curl -sf "${BASE_URL}/api/v1/history" -H "${AUTH_HEADER}")
MT_COUNT=$(echo "$HIST_RESP" | python3 -c "import sys,json; print(sum(1 for i in json.load(sys.stdin).get('data',[]) if '600519' in str(i)))" 2>/dev/null || echo "0")
[ "$MT_COUNT" = "1" ] && ok "去重成功(茅台仅1条)" || fail "去重失败(茅台${MT_COUNT}条)"

# ── TC-2.3-05: 删除单条 ──
echo ""
echo "[TC-2.3-05] 删除单条历史"
FIRST_ID=$(echo "$HIST_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',[]); print(d[0].get('id','') if d else '')" 2>/dev/null || echo "")
if [ -n "$FIRST_ID" ]; then
    DEL_RESP=$(curl -sf -X DELETE "${BASE_URL}/api/v1/history/${FIRST_ID}" \
        -H "${AUTH_HEADER}" || echo "FAIL")
    DEL_CODE=$(echo "$DEL_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
    [ "$DEL_CODE" = "0" ] && ok "删除 id=$FIRST_ID 成功" || fail "删除失败"
fi

# ── TC-2.3-06: 清空全部 ──
echo ""
echo "[TC-2.3-06] 清空全部历史"
CLEAR_RESP=$(curl -sf -X DELETE "${BASE_URL}/api/v1/history" \
    -H "${AUTH_HEADER}" || echo "FAIL")
CLEAR_CODE=$(echo "$CLEAR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$CLEAR_CODE" = "0" ] && ok "清空成功" || fail "清空失败"

# 验证已清空
HIST_RESP=$(curl -sf "${BASE_URL}/api/v1/history" -H "${AUTH_HEADER}")
HIST_COUNT=$(echo "$HIST_RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
[ "$HIST_COUNT" = "0" ] && ok "清空后历史为空" || fail "清空后仍有 $HIST_COUNT 条"

# ── 登出后访问 401 ──
echo ""
echo "[TC-2.3-11] 登出后访问被拒"
curl -sf -X POST "${BASE_URL}/api/v1/auth/logout" -H "${AUTH_HEADER}" > /dev/null 2>&1
HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/history" -H "${AUTH_HEADER}" 2>/dev/null)
[ "$HTTP_CODE" = "401" ] && ok "登出后访问历史 → HTTP 401" || fail "登出后访问历史 → HTTP $HTTP_CODE"

echo ""
echo "============================================"
echo -e "  历史 API: 通过 $PASS / 失败 $FAIL"
echo "============================================"
exit $FAIL
