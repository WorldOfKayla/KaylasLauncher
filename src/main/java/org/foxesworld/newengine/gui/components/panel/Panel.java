package org.foxesworld.newengine.gui.components.panel;

import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.gui.attributes.PanelOptions;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class Panel {

    private final Frame frame;
    public Panel(Frame frame) {
        this.frame = frame;
    }

    public JPanel addPanel(FrameAttributes frameAttributes) {
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawDarkenedBackground(g, frameAttributes);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(null);

        JPanel titleBar = new JPanel();
        titleBar.setBackground(hexToColor("#2b2927c9"));
        titleBar.setBounds(0, 0, frame.getFrame().getWidth(), 30);
        contentPanel.add(titleBar);

        final boolean[] isDragging = {false};
        final int[] xOffset = {0};
        final int[] yOffset = { 0 };

        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isDragging[0] = true;
                xOffset[0] = e.getXOnScreen() - frame.getFrame().getX();
                yOffset[0] = e.getYOnScreen() - frame.getFrame().getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging[0] = false;
            }
        });

        titleBar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging[0]) {
                    int x = e.getXOnScreen() - xOffset[0];
                    int y = e.getYOnScreen() - yOffset[0];
                    frame.getFrame().setLocation(x, y);
                }
            }
        });

        return contentPanel;
    }

    private void drawDarkenedBackground(Graphics g, FrameAttributes frameAttributes) {
        BufferedImage background = ImageUtils.getLocalImage(frameAttributes.backgroundImage);
        g.drawImage(background, 0, 0, null);

        g.setColor(hexToColor(frameAttributes.backgroundBlur));
        g.fillRect(0, 0, this.frame.getScreenSize().width, this.frame.getScreenSize().height);
    }

    public JPanel createGroupPanel(PanelOptions panelOptions, String groupName) {
        JPanel groupPanel = new JPanel();
        groupPanel.setName(groupName);
        groupPanel.setOpaque(panelOptions.opaque);
        groupPanel.setLayout(null);
        groupPanel.setBackground(hexToColor(panelOptions.background));
        groupPanel.setBounds(panelOptions.xPos, panelOptions.yPos, panelOptions.width, panelOptions.height);
        return groupPanel;
    }
}
