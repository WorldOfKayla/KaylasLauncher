package org.foxesworld.newengine.gui.components.panel;

import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.gui.attributes.PanelOptions;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.utils.ActionListener;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class Panel {

    private final Frame frame;
    public Panel(Frame frame) {
        this.frame = frame;
    }

    public JPanel setRootPanel(FrameAttributes frameAttributes) {
        JPanel rootPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawDarkenedBackground(g, frameAttributes);
            }
        };
        rootPanel.setOpaque(false);
        rootPanel.setLayout(null);

        return rootPanel;
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
        if(panelOptions.border != null && !panelOptions.border.equals("")) {
            groupPanel.setBorder(BorderFactory.createLineBorder(hexToColor(panelOptions.border), panelOptions.borderThickness, panelOptions.borderRounded));
        }

        if(panelOptions.listener != null){
            ActionListener actionListener = new ActionListener();
            switch (panelOptions.listener){
                case "dragger" -> {
                    actionListener.addDragListener(groupPanel, frame.getFrame());
                }
            }
        }
        groupPanel.setBounds(panelOptions.xPos, panelOptions.yPos, panelOptions.width, panelOptions.height);
        return groupPanel;
    }
}
