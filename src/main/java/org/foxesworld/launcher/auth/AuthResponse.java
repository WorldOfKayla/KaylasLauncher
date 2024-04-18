package org.foxesworld.launcher.auth;

class AuthResponse {
    private String message,units,uuid,token,type,login;
    private int group;

    public String getMessage() {
        return message;
    }

    public String getUnits() {
        return units;
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
}