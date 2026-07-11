package org.takesome.launcher.backend;

import java.net.URI;

/** Executable policy verification for backend transport security. */
public final class BackendTransportSecurityVerification {
    private BackendTransportSecurityVerification() {
    }

    public static void main(String[] args) {
        BackendTransportSecurity.validateEndpoint(
                URI.create("ws://127.0.0.1:18080/ws/launcher"),
                true,
                true
        );
        BackendTransportSecurity.validateEndpoint(
                URI.create("wss://launcher.example.com/ws/launcher"),
                true,
                false
        );
        expectFailure(() -> BackendTransportSecurity.validateEndpoint(
                URI.create("ws://launcher.example.com/ws/launcher"),
                true,
                true
        ));
        expectFailure(() -> BackendTransportSecurity.validateEndpoint(
                URI.create("http://launcher.example.com/ws/launcher"),
                true,
                false
        ));
        require(
                BackendTransportSecurity.parsePins("aa".repeat(32)).size() == 1,
                "Valid TLS public-key pin was not parsed"
        );
        expectFailure(() -> BackendTransportSecurity.parsePins("not-a-sha256-pin"));
        System.out.println("Backend transport security verification passed.");
    }

    private static void expectFailure(Runnable operation) {
        try {
            operation.run();
            throw new IllegalStateException("Expected transport policy failure");
        } catch (IllegalArgumentException expected) {
            // Expected policy rejection.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
