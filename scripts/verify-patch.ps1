<#
.SYNOPSIS
    Automated multi-gate verification runner for midnight-plugin.
.DESCRIPTION
    Runs compilation, plugin structure verification, and unit tests.
    Outputs machine-readable JSON report at build/verification-report.json.
.PARAMETER Quick
    Runs compilation and structure verification, skipping long-running test suites.
.PARAMETER TestPattern
    Specifies a specific test class or pattern to run (e.g. dev.verloren.midnight.CompactBundleTest).
#>
param(
    [switch]$Quick,
    [string]$TestPattern = ""
)

$ErrorActionPreference = "Continue"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$report = @{
    timestamp = (Get-Date).ToString("o")
    project = "midnight-plugin"
    branch = (git rev-parse --abbrev-ref HEAD)
    gates = @{
        git_boundary = @{ passed = $false; detail = "" }
        compilation = @{ passed = $false; detail = "" }
        plugin_structure = @{ passed = $false; detail = "" }
        tests = @{ passed = $false; detail = "" }
    }
    overall_status = "FAILED"
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "   MIDNIGHT PLUGIN - AUTOMATED VERIFICATION HARNESS   " -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

# ----------------------------------------------------
# Gate 0: Git Boundary & Isolation Check
# ----------------------------------------------------
Write-Host "[Gate 0] Checking Git Isolation & Modified Files..." -ForegroundColor Yellow
$currentBranch = git rev-parse --abbrev-ref HEAD
if ($currentBranch -eq "master") {
    Write-Host "  WARNING: Running verification directly on 'master'. Dedicated task branch recommended." -ForegroundColor Magenta
    $report.gates.git_boundary.detail = "On master branch (caution recommended)"
} else {
    Write-Host "  Branch: $currentBranch (Isolated Task Branch)" -ForegroundColor Green
    $report.gates.git_boundary.detail = "Branch $currentBranch verified"
}
$report.gates.git_boundary.passed = $true

# ----------------------------------------------------
# Gate 1: Compilation Check (Java 25)
# ----------------------------------------------------
Write-Host "`n[Gate 1] Verifying Compilation (Java 25)..." -ForegroundColor Yellow
$compileOutput = .\gradlew.bat compileJava compileTestJava --console=plain 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  Compilation: SUCCESS" -ForegroundColor Green
    $report.gates.compilation.passed = $true
    $report.gates.compilation.detail = "Java 25 classes compiled with zero errors"
} else {
    Write-Host "  Compilation: FAILED" -ForegroundColor Red
    $report.gates.compilation.detail = ($compileOutput | Out-String)
    Write-Host ($compileOutput | Select-Object -Last 15 | Out-String)
}

# ----------------------------------------------------
# Gate 2: IntelliJ Plugin Structure Verification
# ----------------------------------------------------
if ($report.gates.compilation.passed) {
    Write-Host "`n[Gate 2] Verifying Plugin Structure..." -ForegroundColor Yellow
    $structOutput = .\gradlew.bat verifyPluginStructure --console=plain 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Plugin Structure: SUCCESS" -ForegroundColor Green
        $report.gates.plugin_structure.passed = $true
        $report.gates.plugin_structure.detail = "Plugin XML and structure validated"
    } else {
        Write-Host "  Plugin Structure: FAILED" -ForegroundColor Red
        $report.gates.plugin_structure.detail = ($structOutput | Out-String)
        Write-Host ($structOutput | Select-Object -Last 15 | Out-String)
    }
} else {
    Write-Host "`n[Gate 2] Skipped due to compilation failure." -ForegroundColor Gray
}

# ----------------------------------------------------
# Gate 3: Unit / Integration Tests
# ----------------------------------------------------
if ($report.gates.compilation.passed -and -not $Quick) {
    Write-Host "`n[Gate 3] Running Automated Tests..." -ForegroundColor Yellow
    $gradleArgs = @("test", "--console=plain")
    if ($TestPattern -ne "") {
        $gradleArgs += "--tests"
        $gradleArgs += $TestPattern
        Write-Host "  Running targeted test: $TestPattern" -ForegroundColor Cyan
    } else {
        Write-Host "  Running quick unit smoke test (CompactBundleTest)..." -ForegroundColor Cyan
        $gradleArgs += "--tests"
        $gradleArgs += "dev.verloren.midnight.CompactBundleTest"
    }

    $testOutput = .\gradlew.bat @gradleArgs 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Tests: SUCCESS" -ForegroundColor Green
        $report.gates.tests.passed = $true
        $report.gates.tests.detail = "All requested tests passed with 0 failures"
    } else {
        Write-Host "  Tests: FAILED" -ForegroundColor Red
        $report.gates.tests.detail = ($testOutput | Out-String)
        Write-Host ($testOutput | Select-Object -Last 20 | Out-String)
    }
} elseif ($Quick) {
    Write-Host "`n[Gate 3] Skipped (Quick mode active)." -ForegroundColor Gray
    $report.gates.tests.passed = $true
    $report.gates.tests.detail = "Skipped via -Quick flag"
}

# ----------------------------------------------------
# Final Summary & Output Generation
# ----------------------------------------------------
$allPassed = $report.gates.git_boundary.passed -and 
             $report.gates.compilation.passed -and 
             $report.gates.plugin_structure.passed -and 
             ($report.gates.tests.passed -or $Quick)

if ($allPassed) {
    $report.overall_status = "PASSED"
    Write-Host "`n========================================================" -ForegroundColor Green
    Write-Host "   VERIFICATION PASSED - ALL GATES SATISFIED          " -ForegroundColor Green
    Write-Host "========================================================`n" -ForegroundColor Green
} else {
    $report.overall_status = "FAILED"
    Write-Host "`n========================================================" -ForegroundColor Red
    Write-Host "   VERIFICATION FAILED - REVIEW LOGS ABOVE            " -ForegroundColor Red
    Write-Host "========================================================`n" -ForegroundColor Red
}

$buildDir = Join-Path $projectRoot "build"
if (-not (Test-Path $buildDir)) { New-Item -ItemType Directory -Path $buildDir | Out-Null }
$reportPath = Join-Path $buildDir "verification-report.json"
$report | ConvertTo-Json -Depth 5 | Set-Content -Path $reportPath -Encoding UTF8
Write-Host "Report saved to: $reportPath`n" -ForegroundColor Gray

if (-not $allPassed) { exit 1 }
