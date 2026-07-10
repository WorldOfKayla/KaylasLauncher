param(
    [string]$Dll = "build\Release\secure_process.dll",
    [Parameter(Mandatory = $true)]
    [string]$CertificateThumbprint,
    [string]$TimestampUrl = "http://timestamp.digicert.com",
    [string]$Output = "build\Release\secure-process.jvmargs"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $Dll)) {
    throw "DLL not found: $Dll"
}

$signtool = Get-Command signtool.exe -ErrorAction Stop
& $signtool.Source sign /sha1 $CertificateThumbprint /fd SHA256 /tr $TimestampUrl /td SHA256 $Dll
if ($LASTEXITCODE -ne 0) {
    throw "signtool failed with exit code $LASTEXITCODE"
}

& $signtool.Source verify /pa /all /v $Dll
if ($LASTEXITCODE -ne 0) {
    throw "Authenticode verification failed with exit code $LASTEXITCODE"
}

$resolvedDll = (Resolve-Path $Dll).Path
$sha256 = (Get-FileHash -Algorithm SHA256 $resolvedDll).Hash.ToLowerInvariant()

@(
    "-Dkaylas.secureProcess.library=$resolvedDll"
    "-Dkaylas.secureProcess.sha256=$sha256"
    "-Dkaylas.secureProcess.required=true"
    "-Dkaylas.secureProcess.integrityRequired=true"
    "-Dkaylas.secureProcess.authenticodeRequired=true"
) | Set-Content -Encoding UTF8 $Output

Write-Host "Signed and verified $resolvedDll"
Write-Host "SHA-256: $sha256"
Write-Host "Launcher arguments: $Output"
