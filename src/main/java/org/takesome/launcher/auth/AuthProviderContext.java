package org.takesome.launcher.auth;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.launcher.config.Config;

import java.util.Collections;
import java.util.Map;

public record AuthProviderContext(
        Auth auth,
        Launcher launcher,
        Engine engine,
        Config config,
        Map<String, Object> credentials
) {
    public AuthProviderContext {
        credentials = credentials == null ? Collections.emptyMap() : credentials;
    }

    public String credential(String key) {
        Object value = credentials.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String login() {
        return credential("login");
    }

    public String password() {
        return credential("password");
    }

    public String endpoint() {
        String credentialEndpoint = credential("authEndpoint");
        if (credentialEndpoint != null && !credentialEndpoint.isBlank()) {
            return credentialEndpoint;
        }
        return config.getAuthEndpoint();
    }
}
