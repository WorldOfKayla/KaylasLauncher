param(
    [string]$BuildDir = "build",
    [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = "Stop"
$projectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $projectDir
try {
    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        throw "JAVA_HOME is required"
    }

    $cmakeJavaHome = $JavaHome.Replace('\', '/')
    cmake -S . -B $BuildDir -A x64 "-DJAVA_HOME=$cmakeJavaHome"
    if ($LASTEXITCODE -ne 0) {
        throw "CMake configure failed with exit code $LASTEXITCODE"
    }

    cmake --build $BuildDir --config Release
    if ($LASTEXITCODE -ne 0) {
        throw "CMake build failed with exit code $LASTEXITCODE"
    }

    $dll = Join-Path $BuildDir "Release\secure_process.dll"
    if (-not (Test-Path $dll)) {
        throw "SecureProcess build did not produce $dll"
    }

    Write-Host "Built $((Resolve-Path $dll).Path)"
}
finally {
    Pop-Location
}
