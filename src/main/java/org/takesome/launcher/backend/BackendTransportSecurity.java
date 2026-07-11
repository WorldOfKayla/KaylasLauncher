package org.takesome.launcher.backend;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Enforces secure backend transport and optional server public-key pinning. */
final class BackendTransportSecurity {
    private static final String PIN_PROPERTY = "kaylas.backend.tls.publicKeySha256";
    private static final String PIN_ENVIRONMENT = "KAYLAS_BACKEND_TLS_PUBLIC_KEY_SHA256";

    private BackendTransportSecurity() {
    }

    static HttpClient createHttpClient(
            Launcher launcher,
            URI endpoint,
            boolean requireSecureTransport,
            boolean allowInsecureLoopback,
            String configuredPins
    ) {
        Objects.requireNonNull(launcher, "launcher");
        Objects.requireNonNull(endpoint, "endpoint");
        validateEndpoint(endpoint, requireSecureTransport, allowInsecureLoopback);

        Set<String> pins = parsePins(resolvePins(configuredPins));
        if (!pins.isEmpty() && !"wss".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException(
                    "Backend TLS public-key pinning requires a wss:// endpoint"
            );
        }

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .executor(launcher.getExecutorServiceProvider().getExecutorService());
        if (!pins.isEmpty()) {
            builder.sslContext(createPinnedSslContext(pins));
            if (Engine.LOGGER != null) {
                Engine.LOGGER.info(
                        "Backend WSS public-key pinning enabled: pins={}",
                        pins.size()
                );
            }
        }
        return builder.build();
    }

    static void validateEndpoint(
            URI endpoint,
            boolean requireSecureTransport,
            boolean allowInsecureLoopback
    ) {
        String scheme = endpoint.getScheme() == null
                ? ""
                : endpoint.getScheme().toLowerCase(Locale.ROOT);
        if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
            throw new IllegalArgumentException(
                    "Launcher backend endpoint must use ws:// or wss://: " + endpoint
            );
        }
        if (endpoint.getHost() == null || endpoint.getHost().isBlank()) {
            throw new IllegalArgumentException(
                    "Launcher backend endpoint has no host: " + endpoint
            );
        }
        if (!requireSecureTransport || "wss".equals(scheme)) {
            return;
        }
        if (allowInsecureLoopback && isExplicitLoopback(endpoint.getHost())) {
            if (Engine.LOGGER != null) {
                Engine.LOGGER.warn(
                        "Using insecure backend transport for loopback development endpoint: {}",
                        endpoint
                );
            }
            return;
        }
        throw new IllegalArgumentException(
                "Insecure launcher backend transport is forbidden; use wss://: " + endpoint
        );
    }

    private static boolean isExplicitLoopback(String host) {
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized);
    }

    private static String resolvePins(String configuredPins) {
        if (configuredPins != null && !configuredPins.isBlank()) {
            return configuredPins;
        }
        String property = System.getProperty(PIN_PROPERTY, "").trim();
        if (!property.isBlank()) {
            return property;
        }
        String environment = System.getenv(PIN_ENVIRONMENT);
        return environment == null ? "" : environment.trim();
    }

    static Set<String> parsePins(String rawPins) {
        if (rawPins == null || rawPins.isBlank()) {
            return Set.of();
        }
        Set<String> pins = new LinkedHashSet<>();
        Arrays.stream(rawPins.split("[,;\s]+"))
                .map(BackendTransportSecurity::normalizePin)
                .filter(pin -> !pin.isBlank())
                .forEach(pins::add);
        return Set.copyOf(pins);
    }

    private static String normalizePin(String value) {
        String normalized = value == null
                ? ""
                : value.replace(":", "").trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.length() != 64
                || normalized.chars().anyMatch(character ->
                        Character.digit(character, 16) < 0)) {
            throw new IllegalArgumentException(
                    "Backend TLS public-key SHA-256 pin must contain 64 hexadecimal characters"
            );
        }
        return normalized;
    }

    private static SSLContext createPinnedSslContext(Set<String> pins) {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
            );
            factory.init((KeyStore) null);
            X509ExtendedTrustManager defaultTrustManager = Arrays.stream(
                            factory.getTrustManagers()
                    )
                    .filter(X509ExtendedTrustManager.class::isInstance)
                    .map(X509ExtendedTrustManager.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new GeneralSecurityException(
                            "Default X.509 trust manager is unavailable"
                    ));

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(
                    null,
                    new TrustManager[]{
                            new PublicKeyPinningTrustManager(defaultTrustManager, pins)
                    },
                    new SecureRandom()
            );
            return context;
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException(
                    "Unable to initialize pinned backend TLS context",
                    error
            );
        }
    }

    private static final class PublicKeyPinningTrustManager
            extends X509ExtendedTrustManager {
        private final X509ExtendedTrustManager delegate;
        private final Set<String> pins;

        private PublicKeyPinningTrustManager(
                X509ExtendedTrustManager delegate,
                Set<String> pins
        ) {
            this.delegate = delegate;
            this.pins = pins;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            delegate.checkServerTrusted(chain, authType);
            verifyServerPin(chain);
        }

        @Override
        public void checkClientTrusted(
                X509Certificate[] chain,
                String authType,
                Socket socket
        ) throws CertificateException {
            delegate.checkClientTrusted(chain, authType, socket);
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] chain,
                String authType,
                Socket socket
        ) throws CertificateException {
            delegate.checkServerTrusted(chain, authType, socket);
            verifyServerPin(chain);
        }

        @Override
        public void checkClientTrusted(
                X509Certificate[] chain,
                String authType,
                SSLEngine engine
        ) throws CertificateException {
            delegate.checkClientTrusted(chain, authType, engine);
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] chain,
                String authType,
                SSLEngine engine
        ) throws CertificateException {
            delegate.checkServerTrusted(chain, authType, engine);
            verifyServerPin(chain);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }

        private void verifyServerPin(X509Certificate[] chain)
                throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException(
                        "Backend TLS certificate chain is empty"
                );
            }
            try {
                byte[] publicKey = chain[0].getPublicKey().getEncoded();
                String actual = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(publicKey)
                );
                if (!pins.contains(actual)) {
                    throw new CertificateException(
                            "Backend TLS public-key pin mismatch: " + actual
                    );
                }
            } catch (GeneralSecurityException error) {
                if (error instanceof CertificateException certificateException) {
                    throw certificateException;
                }
                throw new CertificateException(
                        "Unable to verify backend TLS public-key pin",
                        error
                );
            }
        }
    }
}
