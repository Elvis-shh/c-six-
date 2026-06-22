# SmartReport Performance Test
param([string]$BaseUrl = "http://localhost:8080", [string]$AiUrl = "http://localhost:8000")

$ErrorActionPreference = "Continue"
$Pass = 0; $Fail = 0

function ok($m) { Write-Host "  [PASS] $m" -ForegroundColor Green; $script:Pass++ }
function no($m) { Write-Host "  [FAIL] $m" -ForegroundColor Red; $script:Fail++ }

function Measure-API($label, $url, $threshold, $method = "GET", $body = $null) {
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        if ($method -eq "POST") {
            $null = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json" -TimeoutSec 10
        } else {
            $null = Invoke-RestMethod -Uri $url -TimeoutSec 10
        }
        $sw.Stop()
        $ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 0)
        if ($ms -le $threshold) { ok "$label : ${ms}ms <= ${threshold}ms" } else { no "$label : ${ms}ms > ${threshold}ms" }
        return $ms
    } catch { no "$label : error - $($_.Exception.Message)"; return -1 }
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Performance Test (Module PF)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# Warmup
try { Invoke-RestMethod -Uri "$BaseUrl/api/v1/search/companies/hot" -TimeoutSec 10 | Out-Null } catch { }
try { Invoke-RestMethod -Uri "$AiUrl/health" -TimeoutSec 5 | Out-Null } catch { }

# TC-PF-01: Search latency (target: <200ms P95)
Write-Host ""
Write-Host "[TC-PF-01] Search API latency" -ForegroundColor Yellow
$times = @()
for ($i = 0; $i -lt 5; $i++) {
    $t = Measure-API "Search #$($i+1)" "$BaseUrl/api/v1/search/companies?q=600519&limit=8" 200
    if ($t -gt 0) { $times += $t }
}
if ($times.Count -gt 0) {
    $avg = [math]::Round(($times | Measure-Object -Average).Average, 0)
    $max = ($times | Measure-Object -Maximum).Maximum
    Write-Host "    Avg=${avg}ms Max=${max}ms" -ForegroundColor Gray
}

# TC-PF-02: KPI latency (target: <500ms P95)
Write-Host ""
Write-Host "[TC-PF-02] KPI API latency" -ForegroundColor Yellow
for ($i = 0; $i -lt 3; $i++) {
    Measure-API "KPI #$($i+1)" "$BaseUrl/api/v1/reports/600519/kpi" 500 | Out-Null
}

# TC-PF-03: AI health (target: <50ms)
Write-Host ""
Write-Host "[TC-PF-03] AI engine health latency" -ForegroundColor Yellow
Measure-API "AI /health" "$AiUrl/health" 50 | Out-Null

# TC-PF-04: RAG latency (target: <500ms)
Write-Host ""
Write-Host "[TC-PF-04] RAG search latency" -ForegroundColor Yellow
Measure-API "RAG search" "$AiUrl/ai/v1/rag/search" 500 "POST" '{"query":"gross margin"}' | Out-Null

# Timeline latency
Write-Host ""
Write-Host "[TC-PF-06] Timeline API latency" -ForegroundColor Yellow
Measure-API "Timeline" "$BaseUrl/api/v1/reports/600519/timeline" 500 | Out-Null

# Concurrency test
Write-Host ""
Write-Host "[Stress] 10 concurrent searches" -ForegroundColor Yellow
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$jobs = @()
for ($i = 0; $i -lt 10; $i++) {
    $jobs += Start-Job -ScriptBlock { param($url) try { Invoke-RestMethod -Uri $url -TimeoutSec 15 | Out-Null; return $true } catch { return $false } } -ArgumentList "$BaseUrl/api/v1/search/companies?q=bank&limit=5"
}
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
$sw.Stop()
$success = ($results | Where-Object { $_ -eq $true }).Count
if ($success -eq 10) { ok "10 concurrent all OK: $([math]::Round($sw.Elapsed.TotalMilliseconds,0))ms" }
else { no "10 concurrent: $success/10 OK" }

Write-Host ""
Write-Host " Performance: Pass=$Pass Fail=$Fail Total=$($Pass+$Fail)" -ForegroundColor Cyan
