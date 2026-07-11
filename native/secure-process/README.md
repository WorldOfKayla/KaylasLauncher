# SecureProcess

Windows-native process hardening, module auditing and launcher attestation for KaylasLauncher.

SecureProcess applies documented Windows process mitigation policies through JNI. It is defensive hardening, not an anti-cheat and not a guarantee against an administrator, kernel-mode code, or a compromised JVM/runtime.

## Baseline profile

- Safe DLL search directories
- DEP
- Bottom-up and high-entropy ASLR
- Legacy extension-point disable policy
- Remote-image loading block
- Low-integrity image loading block
- Strict invalid-handle checks
- Prefer System32 images

The baseline deliberately does **not** enable:

- `ProcessDynamicCodePolicy`, because HotSpot JIT requires executable dynamic code
- `ProcessSignaturePolicy`, because the launcher and third-party JNI libraries may not be Microsoft-signed
- `ProcessChildProcessPolicy`, because the launcher must start Minecraft

## Integrity chain

Before `System.load`, the Java facade can verify:

1. SHA-256 against `kaylas.secureProcess.sha256`
2. Optional detached Ed25519 signature against an embedded/configured X.509 public key

After loading, the native library verifies its Authenticode chain through `WinVerifyTrust`. Strict deployment should enable all three checks.

The Authenticode check happens after loading because the verifier itself lives in the native module. Pre-load publisher verification would require a separate trusted bootstrap module or another native bridge. The SHA-256 and optional Ed25519 signature remain the pre-load trust boundary.

## Current-process module audit

`SecureProcessAudit` requests a module snapshot from the native library using `EnumProcessModulesEx`. Only the launcher process is inspected.

Each module report includes:

- absolute path
- base address
- image size
- Authenticode status
- SHA-256
- trusted-location status
- hash-allowlist status
- final finding classification

`SecureProcessModuleMonitor` periodically compares snapshots and reports newly loaded or changed suspicious modules. It does not unload modules, terminate processes, inject code, or inspect foreign processes.


## Source architecture

The native runtime is split by responsibility. JNI is intentionally a thin transport layer.

```text
src/
|-- secure_process_jni.cpp       JNI exports and DllMain only
|-- core/
|   |-- text.*                   UTF-8, Java strings, JSON and binary encodings
|   `-- error_state.*            Win32 diagnostics and thread-safe last-error state
|-- runtime/
|   `-- process_state.*          module handle and applied mitigation state
|-- mitigation/
|   |-- flags.hpp                stable mitigation bit contract
|   `-- process_mitigation.*     Windows process-policy application
|-- audit/
|   |-- authenticode.*           WinVerifyTrust integration
|   `-- module_audit.*           current-process module inventory
|-- attestation/
|   |-- hashing.*                SHA-256 byte and file hashing
|   |-- cng_key.*                software-backed SP1 CNG signing key
|   |-- tpm_key.*                TPM Platform Crypto Provider and quote creation
|   |-- attestation.*            canonical SP1 evidence construction
|   `-- hardware_attestation.*   canonical SP2 TPM evidence construction
`-- bootstrap/
    |-- bootstrap_evidence.*     parent-process and Authenticode measurement
    `-- bootstrap_main.cpp       pinned native launcher bootstrap
```

No subsystem depends on JNI except `core/text` for Java string conversion. Attestation consumes
runtime state through a narrow API, while audit and mitigation remain independent.

## Build

Requirements:

- Windows 10/11 SDK
- CMake 3.24+
- Visual Studio 2022 C++ toolchain
- JDK 17+

```powershell
.\scripts\build-release.ps1
```

Output:

```text
build\Release\secure_process.dll
```

## Authenticode signing

Use a code-signing certificate installed in the Windows certificate store:

```powershell
.\scripts\sign-release.ps1 `
  -CertificateThumbprint "YOUR_CERTIFICATE_THUMBPRINT"
```

The script:

- signs with SHA-256
- requests an RFC 3161 timestamp
- verifies the resulting Authenticode signature
- computes the final SHA-256 after signing
- writes strict launcher JVM arguments to `build\Release\secure-process.jvmargs`

The hash must be calculated **after** signing because Authenticode changes the DLL bytes.

## Launcher properties

```text
-Dkaylas.secureProcess.library=C:\path\secure_process.dll
-Dkaylas.secureProcess.sha256=<lowercase SHA-256>
-Dkaylas.secureProcess.required=true
-Dkaylas.secureProcess.integrityRequired=true
-Dkaylas.secureProcess.authenticodeRequired=true
```

Optional detached signature:

```text
-Dkaylas.secureProcess.signature=<Base64 Ed25519 signature>
-Dkaylas.secureProcess.publicKey=<Base64 X.509 Ed25519 public key>
```

Module audit policy:

```text
-Dkaylas.secureProcess.auditIntervalSeconds=30
-Dkaylas.secureProcess.trustedModuleRoots=C:\trusted-one;D:\trusted-two
-Dkaylas.secureProcess.allowedModuleSha256=<hash1>,<hash2>
```

Without strict mode, initialization is fail-open and returns a degraded diagnostic result. Production packaging should use strict mode.

## Remote launcher attestation

SecureProcess 0.4.0 provides two explicit challenge-response profiles for KaylasLauncher:

- `SP1`: software-backed non-exportable Windows CNG signing key;
- `SP2`: TPM-backed ECDSA P-256 key, TPM platform quote and measured native bootstrap.

- A persisted ECDSA P-256 signing key is created in the Windows Key Storage Provider.
- The private key is configured as non-exportable.
- Each backend challenge is bound to the WebSocket session and signed together with launcher build identity, process hash, SecureProcess DLL hash, native version, timestamp, process id and mitigation flags.
- The backend verifies the signature, nonce lifetime, anti-replay state, trusted key id and optional build/hash allowlists before issuing an access token.
- The access token is required for protected WebSocket requests and protected HTTP resources.

SP2 remains fail-closed for integrity, signature, measured-bootstrap, PCR-selection and TPM-quote
failures. When the Microsoft Platform Crypto Provider explicitly reports that the TPM is unavailable,
allowed by backend policy.

## Hardware attestation profile (SP2)

SP2 requires:

- TPM 2.0 available through `Microsoft Platform Crypto Provider`;
- a hardware-backed, non-exportable ECDSA P-256 key;
- a TPM platform quote bound to the backend challenge and measurement payload;
- PCRs 0, 2, 4, 7 and 11 by default;
- a native bootstrap compiled with the accepted launcher JAR SHA-256;
- Authenticode trust for the bootstrap and SecureProcess DLL when the backend policy requires it.

Build the release artifacts with a pinned launcher hash:

```powershell
.\scripts\build-release.ps1 -LauncherSha256 <launcher-jar-sha256>
```

Sign both native artifacts:

```powershell
.\scripts\sign-release.ps1 -CertificateThumbprint <thumbprint>
```

The signing script writes JVM arguments selecting `SP2` and the signed bootstrap path.
The backend must independently allowlist the TPM key identifier, launcher build, DLL, bootstrap
and optionally the TPM endorsement-key public hash.

The diagnostic tool:

```text
build/Release/secure_process_tpm_probe.exe
```

creates or opens the TPM key and requests a platform quote. A result such as
`NTE_DEVICE_NOT_READY (0x80090030)` is classified as `TPM_UNAVAILABLE`; an SP2 request may then use
the signed SP1 fallback when both launcher and backend policies allow it. Other TPM errors remain
operational failures and never trigger fallback.
