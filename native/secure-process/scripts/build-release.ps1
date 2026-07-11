param(
    [string]$BuildDir = "build",
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$LauncherSha256 = $env:KAYLAS_LAUNCHER_SHA256
)

$ErrorActionPreference = "Stop"
$projectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $projectDir
try {
    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        throw "JAVA_HOME is required"
    }

    $cmakeJavaHome = $JavaHome.Replace('\', '/')
    $cmakeArgs = @(
        "-S", ".",
        "-B", $BuildDir,
        "-A", "x64",
        "-DJAVA_HOME=$cmakeJavaHome"
    )
    if (-not [string]::IsNullOrWhiteSpace($LauncherSha256)) {
        $cmakeArgs += "-DSECURE_PROCESS_PINNED_LAUNCHER_SHA256=$($LauncherSha256.ToLowerInvariant())"
    }
    & cmake @cmakeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "CMake configure failed with exit code $LASTEXITCODE"
    }

    cmake --build $BuildDir --config Release
    if ($LASTEXITCODE -ne 0) {
        throw "CMake build failed with exit code $LASTEXITCODE"
    }

    $dll = Join-Path $BuildDir "Release\secure_process.dll"
    $bootstrap = Join-Path $BuildDir "Release\secure_process_bootstrap.exe"
    if (-not (Test-Path $dll)) {
        throw "SecureProcess build did not produce $dll"
    }
    if (-not (Test-Path $bootstrap)) {
        throw "SecureProcess build did not produce $bootstrap"
    }

    Write-Host "Built $((Resolve-Path $dll).Path)"
    Write-Host "Built $((Resolve-Path $bootstrap).Path)"
}
finally {
    Pop-Location
}
