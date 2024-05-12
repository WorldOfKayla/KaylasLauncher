package org.foxesworld.launcher.auth;

import java.util.List;

class AuthResponse {
    private String message;
    private String uuid;
    private String token;
    private String type;
    private String login;
    private List<Balance> balance;
    private int group;
    private String colorScheme;

    public String getMessage() {
        return message;
    }

    public List<Balance> getBalance() {
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

    public String getColorScheme() {
        return colorScheme;
    }
}
