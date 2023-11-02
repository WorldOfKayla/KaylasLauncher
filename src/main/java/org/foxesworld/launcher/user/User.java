package org.foxesworld.launcher.user;

import org.foxesworld.launcher.Auth.Auth;

import java.lang.reflect.Field;
import java.util.Map;

public class User {
    private final Auth auth;

    private String login, password, units, token, uuid;

    public User(Auth auth){
        this.auth = auth;
        this.setUserSpace();
    }

    public void setUserSpace(){
        if(this.auth.getEngine().getAuth().isAuthorised()) {
            auth.getEngine().displayPanel("authForm->false|loggedForm->true|devInfo->true");
            for(Map.Entry<String, String> credentials: auth.getAuthCredentials().entrySet()){
                try {
                    Field field = User.class.getDeclaredField(credentials.getKey());
                    if(field.hashCode()!= 0) {
                        field.set(this, credentials.getValue());
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }
        } else {
            auth.getEngine().displayPanel("loggedForm->false|newsForm->true|authForm->true");
        }
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUnits() {
        return units;
    }

    public String getToken() {
        return token;
    }

    public String getUuid() {
        return uuid;
    }
}
