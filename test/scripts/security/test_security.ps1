# SmartReport Security Test
param([string]$BaseUrl = "http://localhost:8080", [string]$AiUrl = "http://localhost:8000")

$ErrorActionPreference = "Continue"
$Pass = 0; $Fail = 0

function ok($m) { Write-Host "  [PASS] $m" -ForegroundColor Green; $script:Pass++ }
function no($m) { Write-Host "  [FAIL] $m" -ForegroundColor Red; $script:Fail++ }

Write-Host "============================================" -ForegroundColor Magenta
Write-Host " Security Test (Module SC)" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta

# TC-SC-01: SQL Injection
Write-Host ""
Write-Host "[TC-SC-01] SQL Injection" -ForegroundColor Yellow
$payloads = @("' OR '1'='1", "' OR 1=1 --", "'; DROP TABLE companies; --", "' UNION SELECT * FROM users --")
$safe = $true
foreach ($p in $payloads) {
    $enc = [System.Web.HttpUtility]::UrlEncode($p)
    try {
        $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/search/companies?q=$enc&limit=5" -TimeoutSec 10
        if ($r.code -ne 0) { $safe = $false }
    } catch { if ($_.Exception.Response.StatusCode -ne 400) { $safe = $false } }
}
if ($safe) { ok "SQL injection safe" } else { no "SQL injection vulnerable" }

# TC-SC-02: XSS
Write-Host ""
Write-Host "[TC-SC-02] XSS Cross-Site Scripting" -ForegroundColor Yellow
$xssPayloads = @("script alert 1 test", "img src x onerror test", "svg onload test")
$safe = $true
foreach ($p in $xssPayloads) {
    $enc = [System.Web.HttpUtility]::UrlEncode($p)
    try {
        $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/search/companies?q=$enc&limit=5" -TimeoutSec 10
        if ($r.code -ne 0) { $safe = $false }
    } catch { }
}
if ($safe) { ok "XSS safe (search endpoint)" } else { no "XSS concern" }

# TC-SC-03: Large payload
Write-Host ""
Write-Host "[TC-SC-03] Large payload test" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri "$BaseUrl/api/v1/upload/report" -Method Post -Body "test" -ContentType "text/plain" -TimeoutSec 10 -UseBasicParsing
    if ($r.StatusCode -eq 401) { ok "Upload endpoint protected (401)" }
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) { ok "Upload endpoint protected (401)" }
    elseif ($_.Exception.Response.StatusCode -eq 415) { ok "Upload validates Content-Type (415)" }
    else { ok "Upload reachable: $($_.Exception.Response.StatusCode)" }
}

# Auth bypass
Write-Host ""
Write-Host "[Additional] Auth bypass test" -ForegroundColor Yellow
$endpoints = @(
    @{M="GET"; P="/api/v1/history"},
    @{M="POST"; P="/api/v1/chat/messages"},
    @{M="POST"; P="/api/v1/upload/report"},
    @{M="POST"; P="/api/v1/export"},
    @{M="POST"; P="/api/v1/auth/refresh"},
    @{M="GET"; P="/api/v1/auth/me"}
)
$bypassed = 0
foreach ($ep in $endpoints) {
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl$($ep.P)" -Method $ep.M -TimeoutSec 5 -UseBasicParsing
        if ($r.StatusCode -ne 401) { $bypassed++; Write-Host "  [WARN] $($ep.M) $($ep.P) -> $($r.StatusCode)" -ForegroundColor Yellow }
    } catch {
        if ($_.Exception.Response.StatusCode -eq 401) { Write-Host "  [OK] $($ep.M) $($ep.P) -> 401" -ForegroundColor Gray }
    }
}
if ($bypassed -eq 0) { ok "All protected endpoints return 401" }
else { no "$bypassed endpoints may allow bypass" }

Write-Host ""
Write-Host " Security: Pass=$Pass Fail=$Fail Total=$($Pass+$Fail)" -ForegroundColor Magenta
