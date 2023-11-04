package org.foxesworld.engine.gui.components.panel;

import org.foxesworld.engine.gui.components.frame.FrameAttributes;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.utils.DragListener;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class Panel {

    private final FrameConstructor frameConstructor;
    public Panel(FrameConstructor frameConstructor) {
        this.frameConstructor = frameConstructor;
    }

    public JPanel setRootPanel(FrameAttributes frameAttributes) {
        JPanel rootPanel = new JPanel(null, true) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawDarkenedBackground(g, frameAttributes);
            }
        };
        //rootPanel.setOpaque(true);
        rootPanel.setName("rootPanel");

        return rootPanel;
    }

    private void drawDarkenedBackground(Graphics g, FrameAttributes frameAttributes) {
        BufferedImage background = ImageUtils.getLocalImage(frameAttributes.backgroundImage);
        g.drawImage(background, 0, 0, null);
        g.setColor(hexToColor(frameAttributes.backgroundBlur));
        g.fillRect(0, 0, this.frameConstructor.getScreenSize().width, this.frameConstructor.getScreenSize().height);
    }

    public JPanel createGroupPanel(PanelOptions panelOptions, String groupName) {
        JPanel groupPanel = new JPanel(null, true);
        groupPanel.setName(groupName);
        groupPanel.setOpaque(panelOptions.opaque);
        groupPanel.setBackground(hexToColor(panelOptions.background));
        if(panelOptions.border != null && !panelOptions.border.equals("")) {
            this.createBorder(groupPanel, panelOptions.border);
        }

        if(panelOptions.listener != null) {
            DragListener dragListener = new DragListener();
            switch (panelOptions.listener){
                case "dragger" -> dragListener.addDragListener(groupPanel, frameConstructor.getFrame());
            }
        }

        String[] bounds = panelOptions.bounds.split(",");
        int posX = Integer.parseInt(bounds[0]);
        int posY = Integer.parseInt(bounds[1]);
        int width = Integer.parseInt(bounds[2]);
        int height = Integer.parseInt(bounds[3]);
        groupPanel.setBounds(posX, posY, width, height);
        return groupPanel;
    }
    private void createBorder(JPanel groupPanel, String border){
        String[] borderData = border.split(",");
        int top = Integer.parseInt(borderData[0]);
        int left = Integer.parseInt(borderData[1]);
        int bottom = Integer.parseInt(borderData[2]);
        int right = Integer.parseInt(borderData[3]);
        Color borderColor = hexToColor(borderData[4]);
        groupPanel.setBorder(new MatteBorder(top, left, bottom, right, borderColor));
    }
}
