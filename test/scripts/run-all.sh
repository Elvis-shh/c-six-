#!/bin/bash
# ============================================================
# SmartReport 一键自动化测试
# 用法: bash test/scripts/run-all.sh [--skip-e2e] [--skip-perf] [--skip-security]
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="$SCRIPT_DIR/../reports"
mkdir -p "$REPORT_DIR"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
SUMMARY_FILE="$REPORT_DIR/summary_${TIMESTAMP}.txt"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

SKIP_E2E=false
SKIP_PERF=false
SKIP_SECURITY=false

for arg in "$@"; do
    case $arg in
        --skip-e2e) SKIP_E2E=true ;;
        --skip-perf) SKIP_PERF=true ;;
        --skip-security) SKIP_SECURITY=true ;;
    esac
done

PASS_TOTAL=0
FAIL_TOTAL=0

run_test_suite() {
    local name="$1"
    local cmd="$2"
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  $name${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    local exit_code=0
    eval "$cmd" 2>&1 | tee -a "$SUMMARY_FILE" || exit_code=$?

    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}  ✅ $name — 全部通过${NC}"
    else
        echo -e "${RED}  ❌ $name — 存在失败 (exit=$exit_code)${NC}"
        FAIL_TOTAL=$((FAIL_TOTAL + 1))
    fi
}

echo "============================================" | tee "$SUMMARY_FILE"
echo " SmartReport 自动化测试" | tee -a "$SUMMARY_FILE"
echo " 开始时间: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "$SUMMARY_FILE"
echo " 报告目录: $REPORT_DIR" | tee -a "$SUMMARY_FILE"
echo "============================================" | tee -a "$SUMMARY_FILE"

# ── 第 0 步: 健康检查 ──
run_test_suite "0️⃣  健康检查" "bash '$SCRIPT_DIR/health-check.sh'"

# ── 第 1 步: 数据库验证 ──
run_test_suite "1️⃣  数据库验证" "bash '$SCRIPT_DIR/db/test_database.sh'"

# ── 第 2 步: API 测试 ──
run_test_suite "2️⃣  搜索 API" "bash '$SCRIPT_DIR/api/test_search.sh'"
run_test_suite "2️⃣  财报 API" "bash '$SCRIPT_DIR/api/test_reports.sh'"
run_test_suite "2️⃣  分析 API" "bash '$SCRIPT_DIR/api/test_analysis.sh'"
run_test_suite "2️⃣  AI 引擎 API" "bash '$SCRIPT_DIR/api/test_ai_engine.sh'"

# ── 第 3 步: 安全测试 ──
if [ "$SKIP_SECURITY" = false ]; then
    run_test_suite "3️⃣  安全测试" "bash '$SCRIPT_DIR/security/test_security.sh'"
else
    echo -e "${YELLOW}  ⏭ 跳过安全测试${NC}"
fi

# ── 第 4 步: E2E 测试 ──
if [ "$SKIP_E2E" = false ]; then
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  4️⃣  E2E 浏览器测试 (Playwright)${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    if command -v npx &>/dev/null; then
        npx playwright test "$SCRIPT_DIR/e2e/smartreport-e2e.spec.ts" \
            --config="$SCRIPT_DIR/e2e/playwright.config.ts" 2>&1 | tee -a "$SUMMARY_FILE" || true
    else
        echo -e "${YELLOW}  ⚠️ npx 不可用，跳过 Playwright E2E 测试${NC}"
        echo "  安装: npm install @playwright/test && npx playwright install chromium"
    fi
else
    echo -e "${YELLOW}  ⏭ 跳过 E2E 测试${NC}"
fi

# ── 第 5 步: 性能测试 ──
if [ "$SKIP_PERF" = false ]; then
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  5️⃣  性能测试 (k6)${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    if command -v k6 &>/dev/null; then
        k6 run "$SCRIPT_DIR/performance/api-load-test.js" 2>&1 | tee -a "$SUMMARY_FILE" || true
    else
        echo -e "${YELLOW}  ⚠️ k6 不可用，跳过性能测试${NC}"
        echo "  安装: choco install k6  (或 https://k6.io/docs/get-started/installation/)"
    fi
else
    echo -e "${YELLOW}  ⏭ 跳过性能测试${NC}"
fi

# ── 汇总 ──
echo "" | tee -a "$SUMMARY_FILE"
echo "============================================" | tee -a "$SUMMARY_FILE"
echo " 测试完成: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "$SUMMARY_FILE"
echo " 报告文件: $SUMMARY_FILE" | tee -a "$SUMMARY_FILE"
echo "============================================" | tee -a "$SUMMARY_FILE"

if [ $FAIL_TOTAL -eq 0 ]; then
    echo -e "${GREEN} 🎉 所有测试套件通过！${NC}"
else
    echo -e "${RED} ⚠️  ${FAIL_TOTAL} 个测试套件存在失败${NC}"
fi

exit $FAIL_TOTAL
