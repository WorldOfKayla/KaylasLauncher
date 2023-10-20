package org.foxesworld.engine.utils.HTTP;

import com.google.gson.annotations.SerializedName;

public class RequestProperty {
    @SerializedName("propertyKey")
    String propertyKey;

    @SerializedName("propertyValue")
    String propertyValue;
}