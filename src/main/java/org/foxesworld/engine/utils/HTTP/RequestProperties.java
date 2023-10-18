package org.foxesworld.engine.utils.HTTP;

import com.google.gson.annotations.SerializedName;

class RequestProperties {
    @SerializedName("propertyKey")
    String propertyKey;

    @SerializedName("propertyValue")
    String getPropertyValue;
}