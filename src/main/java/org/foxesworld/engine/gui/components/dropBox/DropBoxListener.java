package org.foxesworld.engine.gui.components.dropBox;

public interface DropBoxListener {

    void onScrollBoxCreated(int index);
    void onScrollBoxOpen(int index);
    void onScrollBoxClose(int index);
    void onServerHover(int index);
}
