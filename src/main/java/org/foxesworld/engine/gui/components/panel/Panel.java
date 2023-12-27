package org.foxesworld.engine.gui.components.panel;

import org.foxesworld.engine.gui.components.frame.FrameAttributes;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.utils.CurrentMonth;
import org.foxesworld.engine.utils.DragListener;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class Panel extends JPanel {
    private FrameAttributes frameAttributes;
    private JPanel groupPanel;
    private final FrameConstructor frameConstructor;

    public Panel(FrameConstructor frameConstructor) {
        this.frameConstructor = frameConstructor;
    }

    public JPanel setRootPanel(FrameAttributes frameAttributes) {
        this.frameAttributes = frameAttributes;
        JPanel rootPanel = new JPanel(null, true) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawDarkenedBackground(g);
            }
        };
        rootPanel.setOpaque(false);
        rootPanel.setName("rootPanel");

        return rootPanel;
    }

    private void drawDarkenedBackground(Graphics g) {
        BufferedImage backgroundImage = ImageUtils.getLocalImage(getSeasonalBackground());
        g.drawImage(applyDarkening(backgroundImage, hexToColor(frameAttributes.getBackgroundBlur())), 0, 0, null);
    }

    private String getSeasonalBackground(){
        switch(CurrentMonth.getCurrentMonth()){
            case DECEMBER, JANUARY, FEBRUARY:
                return  frameAttributes.getWinterImage();

            case MARCH, APRIL, MAY:
                return  frameAttributes.getSpringImage();

            case JUNE, JULY, AUGUST:
                return  frameAttributes.getSummerImage();

            case SEPTEMBER, OCTOBER, NOVEMBER:
                return  frameAttributes.getAutumnImage();
        }
        return  "";
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

    public JPanel createGroupPanel(PanelAttributes panelOptions, String groupName) {
        groupPanel = new JPanel(null, true) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (panelOptions.getBackgroundImage() != null) {
                    BufferedImage backgroundImage = ImageUtils.getLocalImage(panelOptions.getBackgroundImage());
                    g.drawImage(applyDarkening(backgroundImage, hexToColor(panelOptions.getBackground())), 0, 0, null);
                }

                if(panelOptions.getCornerRadius() != 0){
                    int cornerRadius = panelOptions.getCornerRadius();
                    RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                    g2d.setColor(getBackground());
                    g2d.fill(roundedRectangle);
                    g2d.setColor(getForeground());
                    g2d.draw(roundedRectangle);
                    g2d.dispose();
                }
            }

            @Override
            protected void paintBorder(Graphics g) {
                if (panelOptions.getCornerRadius() != 0) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int cornerRadius = panelOptions.getCornerRadius();

                    RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                            0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

                    g2d.setColor(getForeground());
                    g2d.draw(roundedRectangle);

                    g2d.dispose();
                }
            }
        };

        groupPanel.setName(groupName);
        groupPanel.setOpaque(panelOptions.getCornerRadius() == 0 && panelOptions.isOpaque());
        groupPanel.setBackground(hexToColor(panelOptions.getBackground()));
        if (panelOptions.getBorder() != null && !panelOptions.getBorder().equals("")) {
            this.createBorder(groupPanel, panelOptions.getBorder());
        }

        if (panelOptions.getListener() != null) {
            DragListener dragListener = new DragListener();
            switch (panelOptions.getListener()) {
                case "dragger" -> dragListener.addDragListener(groupPanel, frameConstructor);
            }
        }


        if(panelOptions.isFocusable()) {
            groupPanel.setFocusable(true);
            groupPanel.requestFocus();
        }

        String bounds = panelOptions.getBounds();
        groupPanel.setBounds(getPanelBounds(bounds, 0), getPanelBounds(bounds, 1), getPanelBounds(bounds, 2), getPanelBounds(bounds, 3));
        return groupPanel;
    }

    private int getPanelBounds(String bounds, int index){
        return Integer.parseInt(bounds.split(",")[index]);
    }

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