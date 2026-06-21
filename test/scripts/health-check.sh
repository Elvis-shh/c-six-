#!/bin/bash
# ============================================================
# SmartReport 健康检查脚本
# 验证所有服务是否正常运行
# 用法: bash test/scripts/health-check.sh
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

check() {
    local name="$1"
    local cmd="$2"
    echo -n "  [检查] $name ... "
    if eval "$cmd" &>/dev/null; then
        echo -e "${GREEN}✅ PASS${NC}"
        ((PASS++))
    else
        echo -e "${RED}❌ FAIL${NC}"
        ((FAIL++))
    fi
}

echo "============================================"
echo " SmartReport 健康检查"
echo " $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"
echo ""

# ── Docker 容器状态 ──
echo "── Docker 容器状态 ──"
check "MySQL 容器运行中"          "docker ps --filter name=smartreport-mysql --filter status=running | grep -q smartreport-mysql"
check "Redis 容器运行中"          "docker ps --filter name=smartreport-redis --filter status=running | grep -q smartreport-redis"
check "Backend 容器运行中"        "docker ps --filter name=smartreport-backend --filter status=running | grep -q smartreport-backend"
check "AI Engine 容器运行中"      "docker ps --filter name=smartreport-ai-engine --filter status=running | grep -q smartreport-ai-engine"
check "Frontend 容器运行中"       "docker ps --filter name=smartreport-frontend --filter status=running | grep -q smartreport-frontend"
echo ""

# ── 端口可达性 ──
echo "── 端口可达性 ──"
check "MySQL :3307"              "timeout 3 bash -c 'echo >/dev/tcp/localhost/3307' 2>/dev/null"
check "Backend :8080"            "timeout 3 bash -c 'echo >/dev/tcp/localhost/8080' 2>/dev/null"
check "AI Engine :8000"          "timeout 3 bash -c 'echo >/dev/tcp/localhost/8000' 2>/dev/null"
check "Frontend :3000"           "timeout 3 bash -c 'echo >/dev/tcp/localhost/3000' 2>/dev/null"
echo ""

# ── 服务健康端点 ──
echo "── 服务健康端点 ──"
check "Backend API 响应"         "curl -sf -o /dev/null http://localhost:8080/api/v1/search/companies/hot"
check "AI Engine /health"        "curl -sf -o /dev/null http://localhost:8000/health"
check "Frontend 页面可访问"       "curl -sf -o /dev/null http://localhost:3000"
echo ""

# ── 数据库连接 ──
echo "── 数据库连接 ──"
check "MySQL 可连接"             "docker exec smartreport-mysql mysqladmin ping -h localhost -u root -proot123 2>/dev/null | grep -q 'mysqld is alive'"
check "数据库表存在"              "docker exec smartreport-mysql mysql -u smartreport -psmartreport123 smartreport -e 'SHOW TABLES' 2>/dev/null | grep -q companies"
check "种子数据存在"              "docker exec smartreport-mysql mysql -u smartreport -psmartreport123 smartreport -e \"SELECT COUNT(*) FROM companies\" 2>/dev/null | tail -1 | grep -q -E '^[0-9]+'"
echo ""

# ── 结果汇总 ──
echo "============================================"
TOTAL=$((PASS + FAIL))
echo -e "  总计: ${TOTAL}  通过: ${GREEN}${PASS}${NC}  失败: ${RED}${FAIL}${NC}"
if [ $FAIL -eq 0 ]; then
    echo -e "  ${GREEN}所有服务健康 ✅${NC}"
else
    echo -e "  ${RED}存在 ${FAIL} 项异常，请检查 ❌${NC}"
fi
echo "============================================"

exit $FAIL
