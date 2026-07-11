# SecureProcess Attestation Profiles

KaylasLauncher supports two explicit SecureProcess challenge-response profiles.

## SP1: software attestation

SP1 uses a persisted, non-exportable ECDSA P-256 key in the Windows Key Storage Provider.
It signs the backend challenge, WebSocket session binding, launcher build identity, Java process hash,
SecureProcess DLL hash, native version and mitigation state.

SP1 remains the development and migration profile. It never satisfies an SP2 challenge.

## SP2: TPM hardware attestation

SP2 uses `Microsoft Platform Crypto Provider` and requires a hardware-backed TPM ECDSA P-256 key.
The native layer creates a TPM platform quote bound to the backend challenge and the complete
measurement payload.

The default PCR policy covers:

```text
PCR 0  - firmware and CRTM
PCR 2  - option ROM and related boot components
PCR 4  - boot manager and executable measurements
PCR 7  - Secure Boot policy
PCR 11 - BitLocker/access-control measurements on Windows
mask   - 0x895 / 2197
```

SP2 evidence additionally contains:

- TPM implementation, platform and provider versions;
- TPM version, manufacturer and firmware version;
- endorsement-key public SHA-256;
- TPM quote, quote nonce and quote SHA-256;
- native bootstrap PID, path, SHA-256 and Authenticode status;
- parent-process verification proving that the bootstrap started the JVM.

SP2 is fail-closed. A missing TPM, unavailable Platform Crypto Provider, missing quote,
unknown TPM key, wrong PCR selection, unsigned bootstrap or unregistered artifact causes denial.
There is no automatic fallback to SP1.

## Native bootstrap

`secure_process_bootstrap.exe` is compiled with the SHA-256 of exactly one launcher JAR.
It does not accept an arbitrary child command. It constructs:

```text
<java.exe>
  -Dkaylas.secureProcess.required=true
  -Dkaylas.secureProcess.integrityRequired=true
  -Dkaylas.secureProcess.attestationProfile=SP2
  -Dkaylas.secureProcess.library=<secure_process.dll>
  -Dkaylas.secureProcess.sha256=<dll-sha256>
  -jar <pinned-launcher.jar>
```

`--verify-only` validates the launcher pin and native artifacts without starting the JVM.
The hardware packaging pipeline verifies both a positive pinned-JAR case and a negative foreign-JAR case.

## Build tasks

Standard SP1 verification:

```text
gradlew.bat clean check
```

Unsigned SP2 development package:

```text
gradlew.bat hardwareSecureLauncherDist
```

Signed SP2 package:

```text
gradlew.bat hardwareSecureLauncherDist   -PsecureProcessCertificateThumbprint=<certificate-thumbprint>
```

TPM readiness probe:

```text
gradlew.bat secureProcessTpmProbe
```

The hardware release task always performs a clean, non-cached launcher compilation before hashing.
It produces `build/hardware-release/secure-process-hardware-release.json`.

## Current unsigned development artifacts

```text
SecureProcess version = 0.4.0
SP1 keyId = 83999a6747bb4fe53f8693baa646649bd4ed6a18a20528a628b8c22142ee4b1a
IDE build = 7c29461e62887a84249223433eee9437efb5d475ac34c1c65451227c12dbd79a
release JAR = bd4c6513123498cb68af9864a0da46be230b573a1bd881ebb1cfdb8ce511ed9e
SecureProcess DLL = 6161b93bc75b1a47e1529aa1da89fcd3551ab70170ba6d7288f1c6c269b1328f
unsigned pinned bootstrap = b21f5b46db3d3e160cc32bcb17f834e6e460a511d94387b30a624e74fb39535b
```

Signing changes the native artifact hashes. Backend SP2 allowlists must use the final signed hashes,
not the unsigned values above.

## Current hardware readiness

The current execution context reports:

```text
NTE_DEVICE_NOT_READY (0x80090030)
```

Therefore real TPM key creation and a live platform quote were not available in this session.
The SP2 code, quote parser, cryptographic tests and fail-closed negotiation are implemented,
but production activation requires a provisioned TPM execution context and a trusted code-signing certificate.
