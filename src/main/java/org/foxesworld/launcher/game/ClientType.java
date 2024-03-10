package org.foxesworld.launcher.game;

public enum ClientType {
    fmlclient,
    modified,
    fabricclient;

    public static ClientType getType(String str) {
        if (str != null) {
            for (ClientType clientType : ClientType.values()) {
                if (str.equalsIgnoreCase(clientType.name())) {
                    return clientType;
                }
            }
        }
        throw new IllegalArgumentException("No constant with name " + str + " found for enum ClientType");
    }
}
