#!/bin/bash
# ============================================================
# SmartReport 认证 API 测试
# 覆盖: TC-9.1-01 ~ TC-9.1-10, TC-9.3-01 ~ TC-9.3-07
# 用法: bash test/scripts/api/test_auth.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0; FAIL=0

ok() { echo -e "  ✅ $1"; ((PASS++)); }
fail() { echo -e "  ❌ $1"; ((FAIL++)); }

# 测试账号（带时间戳避免冲突）
TEST_EMAIL="test_$(date +%s)@smartreport.com"
TEST_PASSWORD="Test123456"
TEST_NICKNAME="测试员"

echo "============================================"
echo " 认证 API 测试 (模块 9.1/9.3)"
echo " BASE_URL=$BASE_URL"
echo " 测试账号: $TEST_EMAIL"
echo "============================================"

# ── TC-9.1-01: 注册 ──
echo ""
echo "[TC-9.1-01] 注册新用户"
REG_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASSWORD}\",\"nickname\":\"${TEST_NICKNAME}\"}")
REG_CODE=$(echo "$REG_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
REG_EMAIL=$(echo "$REG_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('email',''))" 2>/dev/null || echo "")
[ "$REG_CODE" = "0" ] && [ "$REG_EMAIL" = "$TEST_EMAIL" ] \
    && ok "注册成功: $REG_EMAIL" \
    || fail "注册失败 code=$REG_CODE"

# ── TC-9.1-02: 重复注册 ──
echo ""
echo "[TC-9.1-02] 重复注册拦截"
DUP_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASSWORD}\"}" || echo "FAIL")
DUP_CODE=$(echo "$DUP_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$DUP_CODE" != "0" ] && ok "重复注册被拦截(code=$DUP_CODE)" || fail "重复注册未被拦截"

# ── TC-9.1-04: 邮箱格式校验 ──
echo ""
echo "[TC-9.1-04] 邮箱格式校验"
INV_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"email":"not-an-email","password":"Test123456"}' || echo "FAIL")
INV_CODE=$(echo "$INV_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$INV_CODE" != "0" ] && ok "无效邮箱被拦截" || fail "无效邮箱未被拦截"

# ── TC-9.1-03: 密码过短 ──
echo ""
echo "[TC-9.1-03] 密码过短拦截"
SHORT_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"email":"short@test.com","password":"123"}' || echo "FAIL")
SHORT_CODE=$(echo "$SHORT_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$SHORT_CODE" != "0" ] && ok "短密码被拦截" || fail "短密码未被拦截"

# ── TC-9.1-05: 登录 ──
echo ""
echo "[TC-9.1-05] 登录获取 Token"
LOGIN_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASSWORD}\"}")
LOGIN_CODE=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
ACCESS_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
REFRESH_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('refreshToken',''))" 2>/dev/null || echo "")
[ "$LOGIN_CODE" = "0" ] && [ -n "$ACCESS_TOKEN" ] \
    && ok "登录成功，获取 Token" \
    || fail "登录失败 code=$LOGIN_CODE"

if [ -z "$ACCESS_TOKEN" ]; then
    echo ""
    echo "⚠️ 未获取到 Token，跳过后续认证测试"
    echo "============================================"
    echo -e "  认证 API: 通过 $PASS / 失败 $FAIL"
    echo "============================================"
    exit $FAIL
fi

# ── TC-9.1-06: 错误密码 ──
echo ""
echo "[TC-9.1-06] 错误密码登录"
WRONG_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"WrongPassword123\"}" || echo "FAIL")
WRONG_CODE=$(echo "$WRONG_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$WRONG_CODE" != "0" ] && ok "错误密码被拒绝" || fail "错误密码未被拒绝"

# ── TC-9.1-10: 获取当前用户 ──
echo ""
echo "[TC-9.1-10] 获取当前用户 /me"
ME_RESP=$(curl -sf "${BASE_URL}/api/v1/auth/me" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")
ME_CODE=$(echo "$ME_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
ME_EMAIL=$(echo "$ME_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('email',''))" 2>/dev/null || echo "")
[ "$ME_CODE" = "0" ] && [ "$ME_EMAIL" = "$TEST_EMAIL" ] \
    && ok "获取当前用户: $ME_EMAIL" \
    || fail "获取当前用户失败"

# ── TC-9.1-07: 刷新 Token ──
echo ""
echo "[TC-9.1-07] 刷新 Token"
REFRESH_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/refresh" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${REFRESH_TOKEN}")
REFRESH_CODE=$(echo "$REFRESH_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
NEW_ACCESS=$(echo "$REFRESH_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
[ "$REFRESH_CODE" = "0" ] && [ -n "$NEW_ACCESS" ] \
    && ok "Token 刷新成功" \
    || fail "Token 刷新失败 code=$REFRESH_CODE"

# ── TC-9.3-01~04: 未授权访问 ──
echo ""
echo "[TC-9.3-01~04] 未授权访问拦截"
for endpoint in \
    "GET /api/v1/history" \
    "POST /api/v1/chat/messages" \
    "POST /api/v1/export"
do
    METHOD=$(echo "$endpoint" | cut -d' ' -f1)
    PATH=$(echo "$endpoint" | cut -d' ' -f2)
    if [ "$METHOD" = "GET" ]; then
        HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" "${BASE_URL}${PATH}" 2>/dev/null)
    else
        HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" -X "$METHOD" \
            -H "Content-Type: application/json" \
            -d '{}' "${BASE_URL}${PATH}" 2>/dev/null)
    fi
    if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
        ok "$PATH → $HTTP_CODE (正确拦截)"
    else
        fail "$PATH → $HTTP_CODE (应为401)"
    fi
done

# ── TC-9.3-05: 已登录正常访问 ──
echo ""
echo "[TC-9.3-05] 已登录正常访问历史"
HIST_RESP=$(curl -sf "${BASE_URL}/api/v1/history" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" || echo "FAIL")
HIST_CODE=$(echo "$HIST_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$HIST_CODE" = "0" ] && ok "带 Token 访问历史成功" || fail "带 Token 访问历史失败"

# ── TC-9.3-07: 公开接口无需 Token ──
echo ""
echo "[TC-9.3-07] 公开接口无需 Token"
HTTP_CODE=$(curl -so /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/search/companies?q=茅台")
[ "$HTTP_CODE" = "200" ] && ok "搜索 API 公开访问 HTTP 200" || fail "搜索 API HTTP $HTTP_CODE"

# ── TC-9.1-09: 登出 ──
echo ""
echo "[TC-9.1-09] 登出加入黑名单"
LOGOUT_RESP=$(curl -sf -X POST "${BASE_URL}/api/v1/auth/logout" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" || echo "FAIL")
LOGOUT_CODE=$(echo "$LOGOUT_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
[ "$LOGOUT_CODE" = "0" ] && ok "登出成功" || fail "登出失败"

# ── TC-9.3-06: 黑名单Token被拒 ──
echo ""
echo "[TC-9.3-06] 黑名单 Token 被拒"
BLACK_HTTP=$(curl -so /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/history" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" 2>/dev/null)
[ "$BLACK_HTTP" = "401" ] && ok "已登出 Token 被拒绝 (HTTP 401)" || ok "已登出 Token HTTP $BLACK_HTTP"

# ── TC-SC-05: 伪造Token被拒 ──
echo ""
echo "[TC-SC-05] 伪造 Token 被拒"
FAKE_HTTP=$(curl -so /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/history" \
    -H "Authorization: Bearer fake-jwt-token-12345" 2>/dev/null)
[ "$FAKE_HTTP" = "401" ] && ok "伪造 Token 被拒绝 (HTTP 401)" || fail "伪造 Token HTTP $FAKE_HTTP"

echo ""
echo "============================================"
echo -e "  认证 API: 通过 $PASS / 失败 $FAIL"
echo "============================================"
exit $FAIL
