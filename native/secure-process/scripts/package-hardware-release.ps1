param(
    [Parameter(Mandatory = $true)]
    [string]$LauncherJar,
    [string]$OutputDir = "build\hardware-release",
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$CertificateThumbprint = "",
    [string]$TimestampUrl = "http://timestamp.digicert.com"
)

$ErrorActionPreference = "Stop"
$projectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$resolvedLauncher = (Resolve-Path $LauncherJar).Path
$launcherHash = (Get-FileHash -Algorithm SHA256 $resolvedLauncher).Hash.ToLowerInvariant()

Push-Location $projectDir
try {
    & (Join-Path $PSScriptRoot "build-release.ps1") `
        -JavaHome $JavaHome `
        -LauncherSha256 $launcherHash
    if ($LASTEXITCODE -ne 0) {
        throw "SecureProcess hardware build failed with exit code $LASTEXITCODE"
    }

    $dll = Join-Path $projectDir "build\Release\secure_process.dll"
    $bootstrap = Join-Path $projectDir "build\Release\secure_process_bootstrap.exe"
    $signed = $false
    if (-not [string]::IsNullOrWhiteSpace($CertificateThumbprint)) {
        & (Join-Path $PSScriptRoot "sign-release.ps1") `
            -Dll $dll `
            -Bootstrap $bootstrap `
            -CertificateThumbprint $CertificateThumbprint `
            -TimestampUrl $TimestampUrl
        if ($LASTEXITCODE -ne 0) {
            throw "SecureProcess signing failed with exit code $LASTEXITCODE"
        }
        $signed = $true
    }

    $javaExecutable = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path $javaExecutable)) {
        throw "Java executable not found: $javaExecutable"
    }
    $verifyArguments = @(
        "--launcher", $resolvedLauncher,
        "--secure-process", $dll,
        "--java", $javaExecutable,
        "--verify-only"
    )
    if ($signed) {
        $verifyArguments += "--require-signature"
    }
    & $bootstrap @verifyArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Pinned bootstrap verification failed with exit code $LASTEXITCODE"
    }

    $wrongLauncher = Join-Path $projectDir "build\bootstrap-negative-control.jar"
    [System.IO.File]::WriteAllText($wrongLauncher, "not-the-trusted-launcher")
    try {
        & $bootstrap `
            --launcher $wrongLauncher `
            --secure-process $dll `
            --java $javaExecutable `
            --verify-only
        if ($LASTEXITCODE -eq 0) {
            throw "Bootstrap accepted an unpinned launcher artifact"
        }
    }
    finally {
        Remove-Item $wrongLauncher -Force -ErrorAction SilentlyContinue
    }

    $resolvedOutput = if ([System.IO.Path]::IsPathRooted($OutputDir)) {
        [System.IO.Path]::GetFullPath($OutputDir)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path $projectDir $OutputDir))
    }
    New-Item -ItemType Directory -Path $resolvedOutput -Force | Out-Null
    Copy-Item $resolvedLauncher (Join-Path $resolvedOutput (Split-Path $resolvedLauncher -Leaf)) -Force
    Copy-Item $dll (Join-Path $resolvedOutput "secure_process.dll") -Force
    Copy-Item $bootstrap (Join-Path $resolvedOutput "secure_process_bootstrap.exe") -Force

    $dllHash = (Get-FileHash -Algorithm SHA256 $dll).Hash.ToLowerInvariant()
    $bootstrapHash = (Get-FileHash -Algorithm SHA256 $bootstrap).Hash.ToLowerInvariant()
    $manifest = [ordered]@{
        schema = "kaylas.secure-process.hardware-release.v1"
        secureProcessVersion = "0.4.0"
        attestationProtocol = "SP2"
        softwareFallbackProtocol = "SP1"
        softwareFallbackReasonCode = "TPM_UNAVAILABLE"
        launcherFile = (Split-Path $resolvedLauncher -Leaf)
        launcherSha256 = $launcherHash
        secureProcessSha256 = $dllHash
        bootstrapSha256 = $bootstrapHash
        bootstrapLauncherPin = $launcherHash
        authenticodeSigned = $signed
        requiredPcrMask = 2197
        requiredPcrs = @(0, 2, 4, 7, 11)
    }
    $manifestPath = Join-Path $resolvedOutput "secure-process-hardware-release.json"
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 $manifestPath

    Write-Host "Hardware release prepared: $resolvedOutput"
    Write-Host "Launcher SHA-256: $launcherHash"
    Write-Host "SecureProcess SHA-256: $dllHash"
    Write-Host "Bootstrap SHA-256: $bootstrapHash"
    Write-Host "Authenticode signed: $signed"
}
finally {
    Pop-Location
}
