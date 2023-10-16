package org.foxesworld.newengine.gui.components.panel;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;

public class RoundedPanel extends JPanel {
    private int cornerRadius;
    private JPanel groupPanel;

    public RoundedPanel(JPanel groupPanel, int cornerRadius) {
        this.cornerRadius = cornerRadius;
        this.groupPanel = groupPanel;
        groupPanel.setBorder(new EmptyBorder(cornerRadius, cornerRadius, cornerRadius, cornerRadius));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        g2.setColor(getForeground());
        g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        g2.dispose();
    }

    public JPanel getGroupPanel() {
        return groupPanel;
    }
}