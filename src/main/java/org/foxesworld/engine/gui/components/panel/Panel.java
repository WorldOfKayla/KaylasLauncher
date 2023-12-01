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

public class Panel extends JPanel {

    private float alpha;
    private JPanel groupPanel;
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
        rootPanel.setOpaque(false);
        rootPanel.setName("rootPanel");

        return rootPanel;
    }

    private void drawDarkenedBackground(Graphics g, FrameAttributes frameAttributes) {
        BufferedImage backgroundImage = ImageUtils.getLocalImage(frameAttributes.backgroundImage);
        g.drawImage(applyDarkening(backgroundImage, hexToColor(frameAttributes.backgroundBlur)), 0, 0, null);
    }

    private BufferedImage applyDarkening(BufferedImage image, Color darkeningColor) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage darkenedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = darkenedImage.createGraphics();

        //Image
        g2d.drawImage(image, 0, 0, null);

        //Color
        g2d.setColor(darkeningColor);
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();
        return darkenedImage;
    }

    public JPanel createGroupPanel(PanelOptions panelOptions, String groupName) {
        groupPanel = new JPanel(null, true) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (panelOptions.getBackgroundImage() != null) {
                    BufferedImage backgroundImage = ImageUtils.getLocalImage(panelOptions.getBackgroundImage());
                    g.drawImage(applyDarkening(backgroundImage, hexToColor(panelOptions.getBackground())), 0, 0, null);
                }
            }
        };
        groupPanel.setName(groupName);
        groupPanel.setOpaque(panelOptions.isOpaque());
        groupPanel.setBackground(hexToColor(panelOptions.getBackground()));
        if (panelOptions.getBorder() != null && !panelOptions.getBorder().equals("")) {
            this.createBorder(groupPanel, panelOptions.getBorder());
        }

        if (panelOptions.getListener() != null) {
            DragListener dragListener = new DragListener();
            switch (panelOptions.getListener()) {
                case "dragger" -> dragListener.addDragListener(groupPanel, frameConstructor.getFrame());
            }
        }


        if(panelOptions.isFocusable()) {
            groupPanel.setFocusable(true);
            groupPanel.requestFocus();
        }

        String[] bounds = panelOptions.getBounds().split(",");
        int posX = Integer.parseInt(bounds[0]);
        int posY = Integer.parseInt(bounds[1]);
        int width = Integer.parseInt(bounds[2]);
        int height = Integer.parseInt(bounds[3]);
        groupPanel.setBounds(posX, posY, width, height);

        return groupPanel;
    }


   /*
    @Override

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
        super.paint(g2d);
        g2d.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Fake the background
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    } */

    private void createBorder(JPanel groupPanel, String border) {
        String[] borderData = border.split(",");
        int top = Integer.parseInt(borderData[0]);
        int left = Integer.parseInt(borderData[1]);
        int bottom = Integer.parseInt(borderData[2]);
        int right = Integer.parseInt(borderData[3]);
        Color borderColor = hexToColor(borderData[4]);
        groupPanel.setBorder(new MatteBorder(top, left, bottom, right, borderColor));
    }
}

