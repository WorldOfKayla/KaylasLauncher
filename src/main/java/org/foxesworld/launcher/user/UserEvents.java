// UserEvents.java
package org.foxesworld.launcher.user;

import java.awt.image.BufferedImage;
import java.util.EventListener;
import java.util.Map;

public interface UserEvents {
    interface UserUpdateListener extends EventListener {
        void onUserDataUpdated(User user);
    }

    interface ServerUpdateListener extends EventListener {
        void onServerUpdated(int serverIndex, String status);
    }

    interface SkinLoadListener extends EventListener {
        void onSkinLoaded(Map<String, BufferedImage> skins);
        void onSkinLoadError(Throwable error);
    }
}