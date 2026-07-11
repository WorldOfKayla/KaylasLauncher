# SecureProcess Backend Attestation

KaylasLauncher does not consider a WebSocket transport usable until SecureProcess attestation succeeds.

The client receives a one-time backend challenge, asks SecureProcess 0.3.0 to measure and sign the current launcher state,
and sends the evidence back. Only `ATTESTATION_OK` completes the backend-bound future used by authentication,
server lists, versions, files and user resources.

The native key is persisted through Windows CNG and marked non-exportable. Evidence includes launcher build identity,
process and DLL hashes, native version, session binding, timestamp and mitigation state.

Useful tasks:

```text
gradlew.bat secureProcessAttestationCheck
gradlew.bat secureProcessBackendHandshakeCheck   -PattestationEndpoint=ws://127.0.0.1:18080/ws/launcher
```

The release shadow JAR is reproducible so its SHA-256 can be placed in the backend allowlist.
Any source or resource change requires rebuilding the JAR and rotating the backend launcher hash.

This is a strong software attestation layer, not a claim of TPM-grade remote attestation.
