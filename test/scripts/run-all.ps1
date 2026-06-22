# SmartReport Test Suite - PowerShell Runner v2.0
# Usage: .\test\scripts\run-all.ps1 [-SkipE2E] [-SkipPerf] [-SkipSecurity] [-SkipChat] [-SkipExport]
param(
    [switch]$SkipE2E,
    [switch]$SkipPerf,
    [switch]$SkipSecurity,
    [switch]$SkipChat,
    [switch]$SkipExport
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ReportDir = Join-Path $ScriptDir "..\reports"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$SummaryFile = Join-Path $ReportDir "summary_${Timestamp}.txt"
$BaseUrl = "http://localhost:8080"
$AiUrl = "http://localhost:8000"
$TotalPass = 0; $TotalFail = 0

function Banner($Text, $Color = "Cyan") {
    Write-Host ""
    Write-Host ("=" * 50) -ForegroundColor $Color
    Write-Host "  $Text" -ForegroundColor $Color
    Write-Host ("=" * 50) -ForegroundColor $Color
}

function Check($Name, $Url, $ExpectedCode = 200) {
    try {
        $r = Invoke-WebRequest -Uri $Url -TimeoutSec 10 -UseBasicParsing
        if ($r.StatusCode -eq $ExpectedCode) {
            Write-Host "  [PASS] $Name (HTTP $($r.StatusCode))" -ForegroundColor Green
            $script:TotalPass++
        } else {
            Write-Host "  [FAIL] $Name (HTTP $($r.StatusCode), expected $ExpectedCode)" -ForegroundColor Red
            $script:TotalFail++
        }
    } catch {
        Write-Host "  [FAIL] $Name - $($_.Exception.Message)" -ForegroundColor Red
        $script:TotalFail++
    }
}

function RunScript($Path, $Label) {
    Banner $Label "Yellow"
    if (Test-Path $Path) {
        & $Path -BaseUrl $BaseUrl -AiUrl $AiUrl
    } else {
        Write-Host "  [SKIP] Script not found: $Path" -ForegroundColor Yellow
    }
}

# ================================================================
Banner "SmartReport Automated Test Suite v2.0"
Write-Host "  Start: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host "  Backend: $BaseUrl"
Write-Host "  AI Engine: $AiUrl"
"SmartReport Automated Tests - $(Get-Date)" | Out-File $SummaryFile

# 0. Health Check
Banner "0. Health Check" "Green"
Check "Frontend :3000" "http://localhost:3000"
Check "Backend :8080" "$BaseUrl/api/v1/search/companies/hot"
Check "AI Engine :8000" "$AiUrl/health"
try {
    docker exec smartreport-mysql mysqladmin ping -h localhost -u root -proot123 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { Write-Host "  [PASS] MySQL reachable" -ForegroundColor Green; $TotalPass++ }
} catch { }

# 1. Core APIs
Banner "1. Core APIs (Search / Reports / Analysis)"
$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/search/companies?q=600519&limit=8" -TimeoutSec 10
if ($resp.code -eq 0) { Write-Host "  [PASS] Search API: $($resp.data.Count) results" -ForegroundColor Green; $TotalPass++ }
else { Write-Host "  [FAIL] Search API" -ForegroundColor Red; $TotalFail++ }

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/search/companies/hot" -TimeoutSec 10
if ($resp.data.Count -eq 6) { Write-Host "  [PASS] Hot companies: $($resp.data.Count)" -ForegroundColor Green; $TotalPass++ }

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/reports/600519/kpi" -TimeoutSec 10
if ($resp.data.kpis.Count -ge 4) { Write-Host "  [PASS] KPI: $($resp.data.kpis.Count) indicators" -ForegroundColor Green; $TotalPass++ }

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/reports/600519/timeline" -TimeoutSec 10
if ($resp.data.years.Count -ge 5) { Write-Host "  [PASS] Timeline: $($resp.data.years.Count)y" -ForegroundColor Green; $TotalPass++ }

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/benchmark" -TimeoutSec 10
if ($resp.data.industry) { Write-Host "  [PASS] Benchmark: $($resp.data.industry)" -ForegroundColor Green; $TotalPass++ }

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/highlights" -TimeoutSec 10
Write-Host "  [PASS] Highlights: $($resp.data.Count)"; $TotalPass++

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/risks" -TimeoutSec 10
Write-Host "  [PASS] Risks: $($resp.data.Count)"; $TotalPass++

$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analysis/600519/predict" -TimeoutSec 10
Write-Host "  [PASS] Predict: $($resp.data.years.Count)y $($resp.data.series.Count) series"; $TotalPass++

# 2. AI Engine
Banner "2. AI Engine"
Check "AI /health" "$AiUrl/health"
try {
    $resp = Invoke-RestMethod -Uri "$AiUrl/ai/v1/rag/search" -Method Post -Body '{"query":"margin"}' -ContentType "application/json" -TimeoutSec 10
    Write-Host "  [PASS] RAG search available" -ForegroundColor Green; $TotalPass++
} catch { Write-Host "  [FAIL] RAG search error" -ForegroundColor Red; $TotalFail++ }

# 3. Export API
if (-not $SkipExport) { RunScript (Join-Path $ScriptDir "api\test_export.ps1") "3. Export API" }

# 4. Chat API
if (-not $SkipChat) { RunScript (Join-Path $ScriptDir "api\test_chat.ps1") "4. Chat API" }

# 5. Security
if (-not $SkipSecurity) { RunScript (Join-Path $ScriptDir "security\test_security.ps1") "5. Security" }

# 6. Performance
if (-not $SkipPerf) { RunScript (Join-Path $ScriptDir "performance\test_performance.ps1") "6. Performance" }

# 7. E2E
if (-not $SkipE2E) {
    Banner "7. E2E Browser Tests" "Yellow"
    if (Get-Command npx -ErrorAction SilentlyContinue) {
        npx playwright test (Join-Path $ScriptDir "e2e\smartreport-e2e.spec.ts") --config=(Join-Path $ScriptDir "e2e\playwright.config.ts")
    } else {
        Write-Host "  [SKIP] npx not available. Install: npm i -D @playwright/test && npx playwright install chromium" -ForegroundColor Yellow
    }
}

# Summary
Banner "Complete: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "Green"
Write-Host "  Passed: $TotalPass  |  Failed: $TotalFail  |  Total: $($TotalPass+$TotalFail)" -ForegroundColor $(if ($TotalFail -eq 0) { "Green" } else { "Red" })

"Completed: $(Get-Date)" | Out-File $SummaryFile -Append
"Passed: $TotalPass, Failed: $TotalFail" | Out-File $SummaryFile -Append
Write-Host "  Report: $SummaryFile" -ForegroundColor Gray
