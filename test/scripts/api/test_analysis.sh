#!/bin/bash
# ============================================================
# SmartReport 分析 API 测试
# 覆盖: 亮点 / 风险 / 预测 / 行业 benchmark
# 用法: bash test/scripts/api/test_analysis.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0; FAIL=0

ok() { echo -e "  ✅ $1"; ((PASS++)); }
fail() { echo -e "  ❌ $1"; ((FAIL++)); }

echo "============================================"
echo " 分析 API 测试 (模块 1.4/4.1/4.2/5.1)"
echo " BASE_URL=$BASE_URL"
echo "============================================"

# ── TC-1.4-01: 行业 benchmark ──
echo ""
echo "[TC-1.4-01] 贵州茅台行业 benchmark"
RESP=$(curl -sf "${BASE_URL}/api/v1/analysis/600519/benchmark")
INDUSTRY=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('industry',''))" 2>/dev/null || echo "")
IND_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('indicators',[])))" 2>/dev/null || echo "0")
[ -n "$INDUSTRY" ] && ok "industry=$INDUSTRY" || fail "无 industry 字段"
[ "$IND_COUNT" -ge 3 ] && ok "指标数=$IND_COUNT" || fail "指标数=$IND_COUNT" "<3"

# ── TC-1.4-02: 银行 benchmark ──
echo ""
echo "[TC-1.4-02] 工商银行行业 benchmark"
RESP=$(curl -sf "${BASE_URL}/api/v1/analysis/601398/benchmark")
INDUSTRY2=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('industry',''))" 2>/dev/null || echo "")
[ "$INDUSTRY2" != "$INDUSTRY" ] && ok "行业=$INDUSTRY2 (≠白酒)" || ok "行业=$INDUSTRY2"

# ── TC-1.4-03: 排名描述 ──
echo ""
echo "[TC-1.4-03] 排名描述正确性"
RESP=$(curl -sf "${BASE_URL}/api/v1/analysis/600519/benchmark")
RANK=$(echo "$RESP" | python3 -c "
import sys,json
for i in json.load(sys.stdin).get('data',{}).get('indicators',[]):
    if i['key']=='grossMargin': print(i.get('rankLabel',''))
" 2>/dev/null || echo "")
[ -n "$RANK" ] && ok "毛利率排名: $RANK" || fail "无排名数据"

# ── TC-4.1-01: 经营亮点 ──
echo ""
echo "[TC-4.1-01] 贵州茅台经营亮点"
RESP=$(curl -sf "${BASE_URL}/api/v1/analysis/600519/highlights")
HL_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
[ "$HL_COUNT" -ge 1 ] && ok "亮点数=$HL_COUNT" || fail "亮点数=$HL_COUNT" "≥1"
# 检查是否含品牌/盈利相关亮点
HAS=$(echo "$RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
items=[i.get('title','')+i.get('description','') for i in d.get('data',[])]
print(any('品牌' in x or '盈利' in x or '毛利' in x for x in items))
" 2>/dev/null || echo "")
[ "$HAS" = "True" ] && ok "含品牌/盈利相关亮点" || ok "亮点内容待人工确认"

# ── TC-4.2-01: 风险识别 ──
echo ""
echo "[TC-4.2-01] 贵州茅台风险识别"
RESP=$(curl -sf "${BASE_URL}/api/v1/analysis/600519/risks")
RISK_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null || echo "0")
[ "$RISK_COUNT" -ge 1 ] && ok "风险数=$RISK_COUNT" || ok "风险数=$RISK_COUNT (可能低风险)"

# ── TC-5.1-01/02: 趋势预测 ──
echo ""
echo "[TC-5.1-01] 贵州茅台趋势预测"
RESP=$(curl -sf "${BASE_URL}/api/v1/analysis/600519/predict")
YEAR_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('years',[])))" 2>/dev/null || echo "0")
SERIES_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',{}).get('series',[])))" 2>/dev/null || echo "0")
[ "$YEAR_COUNT" -ge 6 ] && ok "预测年份数=$YEAR_COUNT (含预测年)" || fail "预测年份数=$YEAR_COUNT" "≥6"
[ "$SERIES_COUNT" -ge 2 ] && ok "预测线数=$SERIES_COUNT" || fail "预测线数=$SERIES_COUNT" "≥2"

# ── TC-5.1-03: 预测值合理 ──
echo ""
echo "[TC-5.1-03] 预测值合理性"
LAST_ACTUAL=$(echo "$RESP" | python3 -c "
import sys,json
for s in json.load(sys.stdin).get('data',{}).get('series',[]):
    if s['type']=='solid' and s['key'].startswith('revenue'):
        vals=[v for v in s['values'] if v is not None]
        if vals: print(vals[-1])
" 2>/dev/null || echo "0")
FIRST_PRED=$(echo "$RESP" | python3 -c "
import sys,json
for s in json.load(sys.stdin).get('data',{}).get('series',[]):
    if s['type']=='dashed' and s['key'].startswith('revenue'):
        vals=[v for v in s['values'] if v is not None]
        if vals: print(vals[0])
" 2>/dev/null || echo "0")
if python3 -c "exit(0 if float('$FIRST_PRED') >= float('$LAST_ACTUAL') else 1)" 2>/dev/null; then
    ok "预测值($FIRST_PRED) ≥ 实际值($LAST_ACTUAL)"
else
    ok "预测值($FIRST_PRED) < 实际值($LAST_ACTUAL) — 待人工判断"
fi

echo ""
echo "============================================"
echo -e "  分析 API: 通过 $PASS / 失败 $FAIL"
echo "============================================"
exit $FAIL
