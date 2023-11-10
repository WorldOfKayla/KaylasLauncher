package org.foxesworld.launcher.Auth;

import com.google.gson.annotations.SerializedName;

class AuthResponse {
    @SerializedName("message")
    String message;

    @SerializedName("units")
    String units;

    @SerializedName("uuid")
    String uuid;

    @SerializedName("token")
    String token;

    @SerializedName("type")
    String type;
}