package org.foxesworld.engine.action.Auth;

import com.google.gson.annotations.SerializedName;

class AuthResponse {
    @SerializedName("message")
    String message;

    @SerializedName("type")
    String type;
}