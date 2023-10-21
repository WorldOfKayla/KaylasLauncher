package org.foxesworld.launcher.user;

import org.foxesworld.launcher.Auth.Auth;

public class User {
    private Auth auth;

    public User(Auth auth){
        this.auth = auth;
        this.setUserSpace();
    }

    public void setUserSpace(){
        if(this.auth.getEngine().isAuthorised()) {
            auth.getEngine().displayPanel("authForm->false|loggedForm->true");
        }
    }
}
