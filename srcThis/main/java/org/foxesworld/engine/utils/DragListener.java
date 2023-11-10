package org.foxesworld.engine.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DragListener {

    public void addDragListener(Component component, JFrame frame) {
        final int[] xOffset = {0};
        final int[] yOffset = {0};
        final boolean[] isDragging = {false};

        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isDragging[0] = true;
                xOffset[0] = e.getXOnScreen() - frame.getX();
                yOffset[0] = e.getYOnScreen() - frame.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging[0] = false;
            }
        });

        component.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging[0]) {
                    int x = e.getXOnScreen() - xOffset[0];
                    int y = e.getYOnScreen() - yOffset[0];
                    frame.setLocation(x, y);
                }
            }
        });
    }

}
