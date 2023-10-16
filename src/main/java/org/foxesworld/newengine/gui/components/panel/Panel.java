package org.foxesworld.newengine.gui.components.panel;

import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.gui.attributes.PanelOptions;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.utils.ActionListener;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.border.MatteBorder;
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
        rootPanel.setName("rootPanel");

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
            this.createBorder(groupPanel, panelOptions.border);
        }

        if(panelOptions.listener != null) {
            ActionListener actionListener = new ActionListener();
            switch (panelOptions.listener){
                case "dragger" -> actionListener.addDragListener(groupPanel, frame.getFrame());
            }
        }
        frame.getAppFrame().displayPanel(groupName, panelOptions.display);

        String[] bounds = panelOptions.bounds.split(",");
        int posX = Integer.parseInt(bounds[0]);
        int posY = Integer.parseInt(bounds[1]);
        int width = Integer.parseInt(bounds[2]);
        int height = Integer.parseInt(bounds[3]);
        groupPanel.setBounds(posX, posY, width, height);
        return groupPanel;
    }
    private JPanel createBorder(JPanel groupPanel, String border){
        String[] borderData = border.split(",");
        int top = Integer.parseInt(borderData[0]);
        int left = Integer.parseInt(borderData[1]);
        int bottom = Integer.parseInt(borderData[2]);
        int right = Integer.parseInt(borderData[3]);
        Color borderColor = hexToColor(borderData[4]);
        groupPanel.setBorder(new MatteBorder(top, left, bottom, right, borderColor));
        return groupPanel;
    }
}
