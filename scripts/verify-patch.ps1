<#
.SYNOPSIS
    Automated multi-gate verification runner for midnight-plugin.
.DESCRIPTION
    Runs compilation, static analysis (Checkstyle & ArchUnit), plugin verification, and unit tests.
    Outputs machine-readable JSON report at build/verification-report.json.
.PARAMETER Quick
    Runs compilation, static analysis, and plugin verification, skipping general unit tests.
.PARAMETER AllTests
    Runs the complete test suite (./gradlew test).
.PARAMETER TestPattern
    Specifies a specific test class or pattern to run (e.g. dev.verloren.midnight.CompactBundleTest).
.PARAMETER StrictBranch
    Fails Gate 0 if running directly on the master branch.
#>
param(
    [switch]$Quick,
    [switch]$AllTests,
    [string]$TestPattern = "",
    [switch]$StrictBranch
)

$ErrorActionPreference = "Continue"
$projectRoot = if ($PSScriptRoot) { Split-Path -Parent $PSScriptRoot } else { (Get-Location).Path }
if (-not (Test-Path (Join-Path $projectRoot "build.gradle.kts"))) {
    $projectRoot = (Get-Location).Path
}
Set-Location $projectRoot

# Cross-platform Gradle wrapper detection
$gradleCmd = if ($IsWindows -or ($env:OS -like "*Windows*")) { ".\gradlew.bat" } else { "./gradlew" }

$report = @{
    timestamp = (Get-Date).ToString("o")
    project = "midnight-plugin"
    branch = (git rev-parse --abbrev-ref HEAD)
    gates = @{
        git_boundary = @{ passed = $false; detail = "" }
        compilation = @{ passed = $false; detail = "" }
        static_analysis = @{ passed = $false; detail = "" }
        plugin_structure = @{ passed = $false; detail = "" }
        tests = @{ passed = $false; detail = ""; test_target = "" }
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
    if ($StrictBranch) {
        Write-Host "  FAILED: Running directly on 'master' with -StrictBranch active." -ForegroundColor Red
        $report.gates.git_boundary.passed = $false
        $report.gates.git_boundary.detail = "Rejected: running directly on master branch"
    } else {
        Write-Host "  WARNING: Running verification directly on 'master'. Dedicated task branch recommended." -ForegroundColor Magenta
        $report.gates.git_boundary.passed = $true
        $report.gates.git_boundary.detail = "On master branch (warning issued)"
    }
} else {
    Write-Host "  Branch: $currentBranch (Isolated Task Branch)" -ForegroundColor Green
    $report.gates.git_boundary.passed = $true
    $report.gates.git_boundary.detail = "Branch $currentBranch verified"
}

# ----------------------------------------------------
# Gate 1: Compilation Check (Java 25)
# ----------------------------------------------------
Write-Host "`n[Gate 1] Verifying Compilation (Java 25)..." -ForegroundColor Yellow
$compileOutput = & $gradleCmd compileJava compileTestJava --console=plain 2>&1
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
# Gate 1b: Static Analysis & Code Style (Checkstyle)
# ----------------------------------------------------
if ($report.gates.compilation.passed) {
    Write-Host "`n[Gate 1b] Verifying Static Analysis & Code Style..." -ForegroundColor Yellow
    $checkstyleOutput = & $gradleCmd checkstyleMain checkstyleTest --console=plain 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Static Analysis: SUCCESS" -ForegroundColor Green
        $report.gates.static_analysis.passed = $true
        $report.gates.static_analysis.detail = "Checkstyle rules verified"
    } else {
        Write-Host "  Static Analysis: FAILED" -ForegroundColor Red
        $report.gates.static_analysis.detail = ($checkstyleOutput | Out-String)
        Write-Host ($checkstyleOutput | Select-Object -Last 15 | Out-String)
    }
} else {
    Write-Host "`n[Gate 1b] Skipped due to compilation failure." -ForegroundColor Gray
}

# ----------------------------------------------------
# Gate 2: IntelliJ Plugin Verification
# ----------------------------------------------------
if ($report.gates.compilation.passed -and $report.gates.static_analysis.passed) {
    Write-Host "`n[Gate 2] Verifying Plugin Specification & Compatibility..." -ForegroundColor Yellow
    $structOutput = & $gradleCmd verifyPluginProjectConfiguration verifyPluginStructure --console=plain 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Plugin Verification: SUCCESS" -ForegroundColor Green
        $report.gates.plugin_structure.passed = $true
        $report.gates.plugin_structure.detail = "Plugin XML, project configuration, and structure validated"
    } else {
        Write-Host "  Plugin Verification: FAILED" -ForegroundColor Red
        $report.gates.plugin_structure.detail = ($structOutput | Out-String)
        Write-Host ($structOutput | Select-Object -Last 15 | Out-String)
    }
} else {
    Write-Host "`n[Gate 2] Skipped due to previous gate failure." -ForegroundColor Gray
}

# ----------------------------------------------------
# Gate 3: Unit / Integration Tests
# ----------------------------------------------------
if ($report.gates.compilation.passed -and $report.gates.static_analysis.passed -and -not $Quick) {
    Write-Host "`n[Gate 3] Running Automated Tests..." -ForegroundColor Yellow
    $gradleArgs = @("test", "--console=plain")
    
    if ($AllTests) {
        Write-Host "  Running complete test suite (all tests)..." -ForegroundColor Cyan
        $report.gates.tests.test_target = "ALL"
    } elseif ($TestPattern -ne "") {
        $gradleArgs += "--tests"
        $gradleArgs += $TestPattern
        Write-Host "  Running targeted test: $TestPattern" -ForegroundColor Cyan
        $report.gates.tests.test_target = $TestPattern
    } else {
        Write-Host "  Running architecture & unit smoke tests..." -ForegroundColor Cyan
        $gradleArgs += "--tests"
        $gradleArgs += "dev.verloren.midnight.architecture.CompactArchitectureTest"
        $gradleArgs += "--tests"
        $gradleArgs += "dev.verloren.midnight.CompactBundleTest"
        $report.gates.tests.test_target = "Architecture & Smoke"
    }

    $testOutput = & $gradleCmd @gradleArgs 2>&1
    if ($LASTEXITCODE -eq 0) {
        if ($AllTests) {
            & $gradleCmd jacocoTestReport --console=plain 2>&1 | Out-Null
        }
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
    $report.gates.tests.test_target = "NONE (Quick)"
}

# ----------------------------------------------------
# Final Summary & Output Generation
# ----------------------------------------------------
$allPassed = $report.gates.git_boundary.passed -and 
             $report.gates.compilation.passed -and 
             $report.gates.static_analysis.passed -and 
             $report.gates.plugin_structure.passed -and 
             $report.gates.tests.passed

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
$reportPath = Join-Path $projectRoot "build\verification-report.json"
$report | ConvertTo-Json -Depth 5 | Out-File -FilePath $reportPath -Encoding utf8 -Force
Write-Host "Report saved to: $reportPath`n" -ForegroundColor Gray

if (-not $allPassed) { exit 1 }
