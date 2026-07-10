# SecureProcess

Windows-native process hardening and current-process module auditing for KaylasLauncher.

SecureProcess applies documented Windows process mitigation policies through JNI. It is defensive hardening, not an anti-cheat and not a guarantee against an administrator, kernel-mode code, or a compromised JVM/runtime.

## Protection profile

- Safe DLL search directories
- DEP
- Bottom-up and high-entropy ASLR
- Legacy extension-point disable policy
- Remote-image loading block
- Low-integrity image loading block
- Strict invalid-handle checks
- Prefer System32 images

The profile deliberately does not enable dynamic-code prohibition, Microsoft-only signatures, or child-process blocking because those policies would break HotSpot JIT, third-party JNI libraries, or launching Minecraft.

## Trust chain

Before `System.load`, the Java facade verifies configured SHA-256 and optional detached Ed25519 signature. After loading, Authenticode is checked through `WinVerifyTrust`.

The module audit uses `EnumProcessModulesEx` only against `GetCurrentProcess()`. It records module path, base address, image size, Authenticode status, SHA-256, trusted location, and allowlist status. Confirmed policy violations cause the launcher to persist an incident record and immediately terminate itself through `Runtime.halt(173)`.

## Build

```powershell
.\scripts\build-release.ps1
```

## Sign

```powershell
.\scripts\sign-release.ps1 -CertificateThumbprint "CERTIFICATE_THUMBPRINT"
```

The signing script timestamps and verifies the DLL, computes the final SHA-256 after signing, and writes strict JVM arguments to `build\Release\secure-process.jvmargs`.
