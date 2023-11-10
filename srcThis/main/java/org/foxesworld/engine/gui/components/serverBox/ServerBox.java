package org.foxesworld.engine.gui.components.serverBox;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ServerBox extends JComponent {
    private static final long serialVersionUID = 1L;

    public BufferedImage img = null;
    public String text = "";

    public ServerBoxStyle sb;

    @Override
    protected void paintComponent(Graphics maing) {
        Graphics2D g = (Graphics2D) maing.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (sb != null) {
            g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
            g.drawString(text, img.getWidth() + 2, img.getHeight() / 2 + g.getFontMetrics().getHeight() / 4);
            g.dispose();
            super.paintComponent(maing);
        }
    }

    public void updateBox(String text, BufferedImage img) {
        this.text = text;
        this.img = img;
        repaint();
    }
}