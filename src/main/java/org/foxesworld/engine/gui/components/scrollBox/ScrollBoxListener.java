package org.foxesworld.engine.gui.components.scrollBox;

public interface ScrollBoxListener {

    void onScrollBoxCreated(int index);
    void onScrollBoxOpen(int index);
    void onScrollBoxClose(int index);
    void onServerHover(int index);
}
