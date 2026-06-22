# SmartReport Export API Test
param([string]$BaseUrl = "http://localhost:8080", [string]$Token = "")

$ErrorActionPreference = "Continue"
$Pass = 0; $Fail = 0; $CompanyCode = "600519"

function ok($m) { Write-Host "  [PASS] $m" -ForegroundColor Green; $script:Pass++ }
function no($m) { Write-Host "  [FAIL] $m" -ForegroundColor Red; $script:Fail++ }

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Export API Test (Module 8.2)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

if (-not $Token) {
    $email = "export_" + (Get-Date -Format "HHmmss") + "@test.com"
    try { $null = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body ('{"email":"' + $email + '","password":"Test123456","nickname":"ExportTest"}') } catch { }
    try { $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType "application/json" -Body ('{"email":"' + $email + '","password":"Test123456"}'); $Token = $r.data.accessToken } catch { Write-Host "  [FAIL] Cannot get token" -ForegroundColor Red; exit 1 }
}

$Headers = @{ "Authorization" = "Bearer $Token"; "Content-Type" = "application/json" }

foreach ($fmt in @("pdf","xlsx","docx","png")) {
    Write-Host ""
    Write-Host "[TC-8.2] Export $fmt" -ForegroundColor Yellow
    $body = '{"companyCode":"' + $CompanyCode + '","format":"' + $fmt + '"}'
    try {
        $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/export" -Method Post -Headers $Headers -Body $body -TimeoutSec 30
        if ($r.code -eq 0) { ok "$fmt export: taskId=$($r.data.taskId)" } else { no "$fmt failed: code=$($r.code)" }
    } catch { no "$fmt error: $($_.Exception.Message)" }
}

Write-Host ""
Write-Host "[TC-8.2-05] Disclaimer export" -ForegroundColor Yellow
$body = '{"companyCode":"' + $CompanyCode + '","format":"pdf","includeDisclaimer":true}'
try { $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/export" -Method Post -Headers $Headers -Body $body -TimeoutSec 30; ok "Disclaimer export OK" } catch { no "Disclaimer: $($_.Exception.Message)" }

Write-Host ""
Write-Host "[TC-8.2-06] No-token export" -ForegroundColor Yellow
$body = '{"companyCode":"' + $CompanyCode + '","format":"pdf"}'
try { $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/export" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 10; no "Not rejected: $($r.code)" }
catch { if ($_.Exception.Response.StatusCode -eq 401) { ok "Returns 401" } else { no "Unexpected: $($_.Exception.Message)" } }

Write-Host ""
Write-Host " Export: Pass=$Pass Fail=$Fail Total=$($Pass+$Fail)" -ForegroundColor Cyan
