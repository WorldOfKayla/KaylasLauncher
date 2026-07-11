package org.takesome.launcher.auth;

import java.util.Map;

/** Verifies backend group aliases used by group-scoped launcher settings. */
public final class LauncherGroupAccessPolicyVerification {
    private LauncherGroupAccessPolicyVerification() {
    }

    public static void main(String[] args) {
        AuthResponse admin = AuthResponse.successBackend(
                "admin-uuid",
                "admin",
                "Administrator",
                "ADMIN"
        );
        AuthResponse user = AuthResponse.successBackend(
                "user-uuid",
                "user",
                "User",
                "USER"
        );

        require(LauncherGroupAccessPolicy.isMember(
                        AuthStatus.AUTHORISED,
                        admin,
                        Map.of(),
                        "admin"
                ),
                "ADMIN role did not satisfy the admin group policy");
        require(LauncherGroupAccessPolicy.isMember(
                        AuthStatus.AUTHORISED,
                        admin,
                        Map.of(),
                        "1"
                ),
                "administrator numeric group id alias was rejected");
        require(LauncherGroupAccessPolicy.isMember(
                        AuthStatus.AUTHORISED,
                        null,
                        Map.of("group", 1.0d),
                        "admin"
                ),
                "numeric credential group did not satisfy the admin policy");
        require(!LauncherGroupAccessPolicy.isMember(
                        AuthStatus.AUTHORISED,
                        user,
                        Map.of(),
                        "admin"
                ),
                "ordinary user received administrator access");
        require(!LauncherGroupAccessPolicy.isMember(
                        AuthStatus.UNAUTHORISED,
                        admin,
                        Map.of(),
                        "admin"
                ),
                "unauthenticated state received administrator access");

        System.out.println("Launcher group access policy verification passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
