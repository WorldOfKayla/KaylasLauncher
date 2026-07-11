param(
    [string]$Dll = "build\Release\secure_process.dll",
    [string]$Bootstrap = "build\Release\secure_process_bootstrap.exe",
    [Parameter(Mandatory = $true)]
    [string]$CertificateThumbprint,
    [string]$TimestampUrl = "http://timestamp.digicert.com",
    [string]$Output = "build\Release\secure-process.jvmargs"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $Dll)) {
    throw "DLL not found: $Dll"
}
if (-not (Test-Path $Bootstrap)) {
    throw "Bootstrap not found: $Bootstrap"
}

$signtool = Get-Command signtool.exe -ErrorAction Stop
foreach ($artifact in @($Dll, $Bootstrap)) {
    & $signtool.Source sign /sha1 $CertificateThumbprint /fd SHA256 /tr $TimestampUrl /td SHA256 $artifact
    if ($LASTEXITCODE -ne 0) {
        throw "signtool failed for $artifact with exit code $LASTEXITCODE"
    }
    & $signtool.Source verify /pa /all /v $artifact
    if ($LASTEXITCODE -ne 0) {
        throw "Authenticode verification failed for $artifact with exit code $LASTEXITCODE"
    }
}

$resolvedDll = (Resolve-Path $Dll).Path
$resolvedBootstrap = (Resolve-Path $Bootstrap).Path
$sha256 = (Get-FileHash -Algorithm SHA256 $resolvedDll).Hash.ToLowerInvariant()
$bootstrapSha256 = (Get-FileHash -Algorithm SHA256 $resolvedBootstrap).Hash.ToLowerInvariant()

@(
    "-Dkaylas.secureProcess.library=$resolvedDll"
    "-Dkaylas.secureProcess.sha256=$sha256"
    "-Dkaylas.secureProcess.required=true"
    "-Dkaylas.secureProcess.integrityRequired=true"
    "-Dkaylas.secureProcess.authenticodeRequired=true"
    "-Dkaylas.secureProcess.attestationProfile=SP2"
    "-Dkaylas.secureProcess.bootstrap=$resolvedBootstrap"
    "-Dkaylas.secureProcess.bootstrapSha256=$bootstrapSha256"
) | Set-Content -Encoding UTF8 $Output

Write-Host "Signed and verified $resolvedDll"
Write-Host "Signed and verified $resolvedBootstrap"
Write-Host "DLL SHA-256: $sha256"
Write-Host "Bootstrap SHA-256: $bootstrapSha256"
Write-Host "Launcher arguments: $Output"
