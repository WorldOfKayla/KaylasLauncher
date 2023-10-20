package org.foxesworld.engine.action.user;

import org.foxesworld.engine.action.Auth.Auth;

public class User {
    private Auth auth;

    public User(Auth auth){
        this.auth = auth;
        this.setUserSpace();
    }

    public void setUserSpace(){
        auth.getEngine().displayPanel("authForm->false|loggedForm->true");
    }
}
