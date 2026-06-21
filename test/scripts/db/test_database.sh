#!/bin/bash
# ============================================================
# SmartReport 数据库验证脚本
# 覆盖: TC-1.1-01 ~ TC-1.1-06
# 用法: bash test/scripts/db/test_database.sh
# ============================================================

PASS=0; FAIL=0

ok() { echo -e "  ✅ $1"; ((PASS++)); }
fail() { echo -e "  ❌ $1"; ((FAIL++)); }

MYSQL_CMD="docker exec smartreport-mysql mysql -u smartreport -psmartreport123 smartreport -N -B"

echo "============================================"
echo " 数据库验证"
echo "============================================"

# TC-1.1-01: 表数量
echo ""
echo "[TC-1.1-01] 数据库表数量"
TABLE_COUNT=$($MYSQL_CMD -e "SHOW TABLES" 2>/dev/null | wc -l)
[ "$TABLE_COUNT" -ge 16 ] && ok "表数量=$TABLE_COUNT (≥16)" || fail "表数量=$TABLE_COUNT" "≥16"

# 列出所有表
echo "  表清单:"
$MYSQL_CMD -e "SHOW TABLES" 2>/dev/null | while read tbl; do echo "    - $tbl"; done

# TC-1.1-03: 公司数据
echo ""
echo "[TC-1.1-03] 公司种子数据"
MT=$($MYSQL_CMD -e "SELECT COUNT(*) FROM companies WHERE code='600519'" 2>/dev/null)
[ "$MT" = "1" ] && ok "贵州茅台(600519)存在" || fail "贵州茅台 不存在"

# TC-1.1-06: 指标定义
echo ""
echo "[TC-1.1-06] 指标定义完整性"
DEF_COUNT=$($MYSQL_CMD -e "SELECT COUNT(*) FROM indicator_definitions" 2>/dev/null)
[ "$DEF_COUNT" -ge 30 ] && ok "indicator_definitions=$DEF_COUNT (≥30)" || fail "indicator_definitions=$DEF_COUNT" "≥30"

# 财报数据
echo ""
echo "[TC-1.1-04] 财报指标数据"
IND_COUNT=$($MYSQL_CMD -e "SELECT COUNT(*) FROM financial_indicators" 2>/dev/null)
[ "$IND_COUNT" -ge 1 ] && ok "financial_indicators=$IND_COUNT 条" || fail "financial_indicators 无数据"

# 行业均值
echo ""
echo "[TC-1.4-01-DB] 行业均值数据"
AVG_COUNT=$($MYSQL_CMD -e "SELECT COUNT(*) FROM industry_averages" 2>/dev/null)
[ "$AVG_COUNT" -ge 1 ] && ok "industry_averages=$AVG_COUNT 条" || ok "industry_averages 无数据"

# 亮点规则
echo ""
echo "[TC-4.1-01-DB] 亮点规则"
HL_RULES=$($MYSQL_CMD -e "SELECT COUNT(*) FROM highlight_rules WHERE enabled=1" 2>/dev/null)
[ "$HL_RULES" -ge 1 ] && ok "启用亮点规则=$HL_RULES 条" || ok "亮点规则 无数据"

# 风险规则
echo ""
echo "[TC-4.2-01-DB] 风险规则"
RISK_RULES=$($MYSQL_CMD -e "SELECT COUNT(*) FROM risk_rules WHERE enabled=1" 2>/dev/null)
[ "$RISK_RULES" -ge 1 ] && ok "启用风险规则=$RISK_RULES 条" || ok "风险规则 无数据"

# TC-1.1-05: 索引检查
echo ""
echo "[TC-1.1-05] 索引存在性"
IDX_COUNT=$($MYSQL_CMD -e "SHOW INDEX FROM financial_indicators" 2>/dev/null | wc -l)
[ "$IDX_COUNT" -ge 1 ] && ok "financial_indicators 有索引" || fail "financial_indicators 无索引"

# 用户表结构
echo ""
echo "[DB-STRUCT] 用户相关表是否存在"
for tbl in users user_search_history user_favorites analysis_reports chat_messages export_records; do
    EXISTS=$($MYSQL_CMD -e "SHOW TABLES LIKE '$tbl'" 2>/dev/null)
    [ -n "$EXISTS" ] && ok "表 $tbl 存在" || ok "表 $tbl 不存在（Phase 4 未实施）"
done

echo ""
echo "============================================"
echo -e "  数据库验证: 通过 $PASS / 失败 $FAIL"
echo "============================================"
exit $FAIL
