package org.takesome.launcher.auth;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class AuthResponse {
    private String message, uuid, token, type, login, groupName, userFullName;
    private List<Map<String, Integer>> balance;
    private int group;
    private String colorScheme;
    private boolean backendManaged;
    private boolean clearStoredCredentialsOnFailure = true;

    public static AuthResponse successNoPassword(String login) {
        AuthResponse response = new AuthResponse();
        response.type = "success";
        response.message = "No-password authorization completed.";
        response.login = login;
        response.uuid = "local-" + UUID.nameUUIDFromBytes(login.getBytes());
        response.token = "no-password-" + UUID.randomUUID();
        response.group = 0;
        response.groupName = "local";
        response.userFullName = login;
        response.colorScheme = "default";
        response.balance = Collections.emptyList();
        response.backendManaged = false;
        response.clearStoredCredentialsOnFailure = false;
        return response;
    }

    public static AuthResponse successBackend(String uuid, String login, String displayName, String role) {
        String resolvedLogin = login == null || login.isBlank() ? "unknown" : login;
        String resolvedDisplayName = displayName == null || displayName.isBlank() ? resolvedLogin : displayName;
        String resolvedRole = role == null || role.isBlank() ? "USER" : role;

        AuthResponse response = new AuthResponse();
        response.type = "success";
        response.message = "Backend authorization completed.";
        response.login = resolvedLogin;
        response.uuid = uuid == null || uuid.isBlank() ? "backend-" + UUID.nameUUIDFromBytes(resolvedLogin.getBytes()) : uuid;
        response.token = "backend-" + UUID.randomUUID();
        response.group = "ADMIN".equalsIgnoreCase(resolvedRole) ? 1 : 0;
        response.groupName = resolvedRole;
        response.userFullName = resolvedDisplayName;
        response.colorScheme = "default";
        response.balance = Collections.emptyList();
        response.backendManaged = true;
        response.clearStoredCredentialsOnFailure = false;
        return response;
    }

    public static AuthResponse failure(String message) {
        return failure(message, true);
    }

    public static AuthResponse failure(String message, boolean clearStoredCredentialsOnFailure) {
        AuthResponse response = new AuthResponse();
        response.type = "error";
        response.message = message;
        response.balance = Collections.emptyList();
        response.backendManaged = false;
        response.clearStoredCredentialsOnFailure = clearStoredCredentialsOnFailure;
        return response;
    }

    public static AuthResponse transientFailure(String message) {
        return failure(message, false);
    }

    public String getMessage() { return message; }
    public List<Map<String, Integer>> getBalance() { return balance == null ? Collections.emptyList() : balance; }
    public String getUuid() { return uuid; }
    public String getToken() { return token; }
    public String getType() { return type; }
    public String getLogin() { return login; }
    public int getGroup() { return group; }
    public String getGroupName() { return groupName; }
    public String getColorScheme() { return colorScheme; }
    public String getUserFullName() { return userFullName; }
    public boolean isBackendManaged() { return backendManaged; }
    public boolean shouldClearStoredCredentialsOnFailure() { return clearStoredCredentialsOnFailure; }
}
