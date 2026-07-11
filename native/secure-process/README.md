# SecureProcess

Windows-native process hardening, module auditing and launcher attestation for KaylasLauncher.

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

## Remote launcher attestation

SecureProcess 0.3.0 adds a Windows CNG-backed challenge-response identity for KaylasLauncher.

- A persisted ECDSA P-256 signing key is created in the Windows Key Storage Provider.
- The private key is configured as non-exportable.
- Each backend challenge is bound to the WebSocket session and signed together with launcher build identity, process hash, SecureProcess DLL hash, native version, timestamp, process id and mitigation flags.
- The backend verifies the signature, nonce lifetime, anti-replay state, trusted key id and optional build/hash allowlists before issuing an access token.
- The access token is required for protected WebSocket requests and protected HTTP resources.

This raises the cost of protocol emulation and copied static identifiers, but it is not equivalent to hardware TPM remote attestation and cannot defeat a fully compromised administrator or kernel.
