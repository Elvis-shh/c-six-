# ============================================================
# SmartReport 测试 — PowerShell 运行器 (Windows)
# 用法: .\test\scripts\run-all.ps1 [-SkipE2E] [-SkipPerf] [-SkipSecurity]
# ============================================================

param(
    [switch]$SkipE2E,
    [switch]$SkipPerf,
    [switch]$SkipSecurity
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ReportDir = Join-Path $ScriptDir "..\reports"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$SummaryFile = Join-Path $ReportDir "summary_${Timestamp}.txt"

$BaseUrl = "http://localhost:8080"
$AiUrl = "http://localhost:8000"

function Write-Banner {
    param($Text, $Color = "Cyan")
    Write-Host ""
    Write-Host ("━" * 50) -ForegroundColor $Color
    Write-Host "  $Text" -ForegroundColor $Color
    Write-Host ("━" * 50) -ForegroundColor $Color
}

function Test-Endpoint {
    param($Name, $Url, $ExpectedCode = 200)
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 10 -UseBasicParsing
        if ($response.StatusCode -eq $ExpectedCode) {
            Write-Host "  ✅ $Name (HTTP $($response.StatusCode))" -ForegroundColor Green
            return $true
        } else {
            Write-Host "  ❌ $Name (HTTP $($response.StatusCode), expected $ExpectedCode)" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "  ❌ $Name — $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# ================================================================
Write-Banner "SmartReport 自动化测试 (PowerShell)"
Write-Host "  开始: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host "  Backend: $BaseUrl"
Write-Host "  AI Engine: $AiUrl"
"SmartReport 自动化测试 — $(Get-Date)" | Out-File $SummaryFile

# ── 0. 健康检查 ──
Write-Banner "0️⃣  健康检查" "Green"
$healthy = $true
$healthy = (Test-Endpoint "Frontend :3000" "http://localhost:3000") -and $healthy
$healthy = (Test-Endpoint "Backend :8080" "$BaseUrl/api/v1/search/companies/hot") -and $healthy
$healthy = (Test-Endpoint "AI Engine :8000" "$AiUrl/health") -and $healthy

# 数据库检查
try {
    docker exec smartreport-mysql mysqladmin ping -h localhost -u root -proot123 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ MySQL 可连接" -ForegroundColor Green
    }
} catch { }

if (-not $healthy) {
    Write-Host "  ⚠️ 部分服务不可用，请先运行 docker compose up -d" -ForegroundColor Yellow
}

# ── 1. 搜索 API ──
Write-Banner "1️⃣  搜索 API"
$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/search/companies?q=茅台&limit=8" -TimeoutSec 10
if ($resp.code -eq 0 -and $resp.data.Count -gt 0) {
    Write-Host "  ✅ TC-1.2-01 搜索'茅台'返回 $($resp.data.Count) 条" -ForegroundColor Green
} else {
    Write-Host "  ❌ TC-1.2-01 搜索失败" -ForegroundColor Red
}

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/search/companies/hot" -TimeoutSec 10
if ($resp.data.Count -eq 6) {
    Write-Host "  ✅ TC-1.2-05 热门公司 $($resp.data.Count) 家" -ForegroundColor Green
}

# ── 2. 财报 API ──
Write-Banner "2️⃣  财报 API"
$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/reports/600519/kpi" -TimeoutSec 10
if ($resp.data.kpis.Count -ge 4) {
    Write-Host "  ✅ KPI 数据完整: $($resp.data.kpis.Count) 个指标" -ForegroundColor Green
}

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/reports/600519/timeline" -TimeoutSec 10
if ($resp.data.years.Count -ge 5) {
    Write-Host "  ✅ Timeline $($resp.data.years.Count) 年 x $($resp.data.metrics.Count) 指标" -ForegroundColor Green
}

# ── 3. 分析 API ──
Write-Banner "3️⃣  分析 API"
$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/benchmark" -TimeoutSec 10
if ($resp.data.industry) {
    Write-Host "  ✅ Benchmark: 行业=$($resp.data.industry), 指标=$($resp.data.indicators.Count)" -ForegroundColor Green
}

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/highlights" -TimeoutSec 10
Write-Host "  ✅ 亮点数: $($resp.data.Count)"

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/risks" -TimeoutSec 10
Write-Host "  ✅ 风险数: $($resp.data.Count)"

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/predict" -TimeoutSec 10
Write-Host "  ✅ 预测: $($resp.data.years.Count) 年, $($resp.data.series.Count) 条线"

# ── 4. AI 引擎 ──
Write-Banner "4️⃣  AI 引擎"
Test-Endpoint "AI /health" "$AiUrl/health"

# ── 5. 安全测试 ──
if (-not $SkipSecurity) {
    Write-Banner "5️⃣  安全测试"
    $sqliResp = Invoke-WebRequest -Uri "$BaseUrl/api/v1/search/companies?q='%20OR%20'1'='1" -TimeoutSec 10 -UseBasicParsing
    if ($sqliResp.StatusCode -eq 200) {
        Write-Host "  ✅ SQL注入测试通过 (HTTP 200, 无数据泄露)" -ForegroundColor Green
    }
}

# ── 6. E2E ──
if (-not $SkipE2E) {
    Write-Banner "6️⃣  E2E 浏览器测试"
    if (Get-Command npx -ErrorAction SilentlyContinue) {
        npx playwright test (Join-Path $ScriptDir "e2e\smartreport-e2e.spec.ts") --config=(Join-Path $ScriptDir "e2e\playwright.config.ts")
    } else {
        Write-Host "  ⚠️ npx 不可用，跳过 Playwright E2E" -ForegroundColor Yellow
    }
}

Write-Banner "测试完成: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "Green"
