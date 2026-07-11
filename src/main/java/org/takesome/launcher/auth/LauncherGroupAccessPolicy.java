package org.takesome.launcher.auth;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Resolves launcher feature access from authenticated backend group data. */
public final class LauncherGroupAccessPolicy {
    private static final Set<String> ADMIN_ALIASES = Set.of(
            "1",
            "admin",
            "administrator"
    );

    private LauncherGroupAccessPolicy() {
    }

    /**
     * Tests whether the authenticated user belongs to the requested group.
     *
     * <p>Backend numeric group id {@code 1} and textual roles {@code admin} and
     * {@code administrator} are treated as equivalent administrator identities.</p>
     */
    public static boolean isMember(Auth auth, String requiredGroup) {
        if (auth == null) {
            return false;
        }
        return isMember(
                auth.getAuthStatus(),
                auth.getAuthResponse(),
                auth.getAuthCredentials(),
                requiredGroup
        );
    }

    static boolean isMember(AuthStatus status,
                            AuthResponse response,
                            Map<String, Object> credentials,
                            String requiredGroup) {
        if (status != AuthStatus.AUTHORISED) {
            return false;
        }
        String required = normalize(requiredGroup);
        if (required.isEmpty()) {
            return false;
        }

        Set<String> identities = identities(response, credentials);
        if (identities.contains(required)) {
            return true;
        }
        return ADMIN_ALIASES.contains(required)
                && identities.stream().anyMatch(ADMIN_ALIASES::contains);
    }

    static Set<String> identities(AuthResponse response, Map<String, Object> credentials) {
        Set<String> identities = new LinkedHashSet<>();
        if (response != null) {
            add(identities, response.getGroupName());
            add(identities, response.getGroup());
        }

        if (credentials != null) {
            add(identities, credentials.get("group"));
            add(identities, credentials.get("groupName"));
            add(identities, credentials.get("role"));
        }
        return Set.copyOf(identities);
    }

    private static void add(Set<String> identities, Object value) {
        String normalized = normalize(value);
        if (!normalized.isEmpty()) {
            identities.add(normalized);
        }
    }

    private static String normalize(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number number) {
            double numeric = number.doubleValue();
            if (Double.isFinite(numeric) && numeric == Math.rint(numeric)) {
                return Long.toString((long) numeric);
            }
        }
        return String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }
}
