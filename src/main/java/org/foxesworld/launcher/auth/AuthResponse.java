package org.foxesworld.launcher.auth;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
class AuthResponse {
    private String message, uuid, token, type,login, groupName, userFullName;
    private List<Map<String, Integer>> balance;
    private int group;
    private String colorScheme;

    public String getMessage() {
        return message;
    }

    public List<Map<String, Integer>> getBalance() {
        return balance;
    }

    public String getUuid() {
        return uuid;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public String getLogin() {
        return login;
    }

    public int getGroup() {
        return group;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getColorScheme() {
        return colorScheme;
    }

    public String getUserFullName() {
        return userFullName;
    }
}