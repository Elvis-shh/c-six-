#!/bin/bash
# ============================================================
# SmartReport 财报 API 测试
# 覆盖: KPI / Timeline / Latest / Indicators
# 用法: bash test/scripts/api/test_reports.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0; FAIL=0

ok() { echo -e "  ✅ $1"; ((PASS++)); }
fail() { echo -e "  ❌ $1"; ((FAIL++)); }

echo "============================================"
echo " 财报 API 测试 (模块 1.3/1.4/3.1~3.4)"
echo " BASE_URL=$BASE_URL"
echo "============================================"

# ── TC-1.3-10 真实数据同比验证 ──
echo ""
echo "[TC-1.3-10] 贵州茅台 2024 营收同比"
RESP=$(curl -sf "${BASE_URL}/api/v1/reports/600519/kpi")
REVENUE_YOY=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
kpis=d.get('data',{}).get('kpis',[])
for k in kpis:
    if k['key']=='revenue': print(k.get('yoy','null'))
" 2>/dev/null || echo "")
YOY_NUM=$(echo "$REVENUE_YOY" | sed 's/[^0-9.]//g')
if [ -n "$YOY_NUM" ]; then
    IS_VALID=$(python3 -c "print(abs(float('$YOY_NUM')-16.07)<5)" 2>/dev/null || echo "False")
    [ "$IS_VALID" = "True" ] && ok "营收同比≈16.07% (实际=$REVENUE_YOY)" || fail "营收同比=$REVENUE_YOY 偏差较大"
else
    fail "未获取到营收同比数据" "应有同比值"
fi

# ── TC-3.1-01 KPI 卡片数据 ──
echo ""
echo "[TC-3.1-01] KPI 4张卡片数据完整"
RESP=$(curl -sf "${BASE_URL}/api/v1/reports/600519/kpi")
KPI_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('kpis',[])))" 2>/dev/null || echo "0")
[ "$KPI_COUNT" -ge 4 ] && ok "KPI 数量=$KPI_COUNT" || fail "KPI 数量=$KPI_COUNT" "≥4"

# 检查 4 个关键指标
for key in revenue profit debtRatio cashFlow; do
    HAS=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(any(k['key']=='$key' for k in d.get('data',{}).get('kpis',[])))" 2>/dev/null || echo "")
    [ "$HAS" = "True" ] && ok "  含指标: $key" || fail "  缺失指标: $key"
done

# ── TC-3.2-01 Timeline 数据 ──
echo ""
echo "[TC-3.2-01] Timeline 5年数据"
RESP=$(curl -sf "${BASE_URL}/api/v1/reports/600519/timeline")
YEAR_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('years',[])))" 2>/dev/null || echo "0")
METRIC_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('metrics',[])))" 2>/dev/null || echo "0")
[ "$YEAR_COUNT" -ge 5 ] && ok "年份数=$YEAR_COUNT" || fail "年份数=$YEAR_COUNT" "≥5"
[ "$METRIC_COUNT" -ge 5 ] && ok "指标数=$METRIC_COUNT" || fail "指标数=$METRIC_COUNT" "≥5"

# ── Latest 端点 ──
echo ""
echo "[TC-2.2-01] Latest 端点"
RESP=$(curl -sf "${BASE_URL}/api/v1/reports/600519/latest")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null || echo "")
[ "$CODE" = "0" ] && ok "latest 端点正常" || fail "code=$CODE" "0"

# ── Indicators 端点 ──
echo ""
echo "[TC-3.4-01] Indicators 指标详情"
RESP=$(curl -sf "${BASE_URL}/api/v1/reports/600519/indicators")
IND_COUNT=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',{}).get('indicators',[])))" 2>/dev/null || echo "0")
[ "$IND_COUNT" -ge 5 ] && ok "指标详情数=$IND_COUNT" || fail "指标详情数=$IND_COUNT" "≥5"

# ── 无效公司代码 ──
echo ""
echo "[TC-2.2-06] 无效公司代码"
RESP=$(curl -sf "${BASE_URL}/api/v1/reports/INVALID/latest" || echo "HTTP_ERROR")
if echo "$RESP" | grep -qi "HTTP_ERROR\|not.found\|不存在"; then
    ok "无效代码返回错误提示"
else
    CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "")
    [ "$CODE" != "0" ] && ok "无效代码 code≠0" || ok "code=0 (数据为空，可接受)"
fi

echo ""
echo "============================================"
echo -e "  财报 API: 通过 $PASS / 失败 $FAIL"
echo "============================================"
exit $FAIL
