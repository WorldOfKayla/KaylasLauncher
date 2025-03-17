package org.foxesworld.test;

import javax.swing.*;
import java.awt.*;
public class SwingSpinner extends JComponent {
    private double angle = 0;
    private double arcLength = 120;
    private double arcSpeed = 2;
    private double rotationSpeed = 820;
    private boolean expanding = true;

    public SwingSpinner() {
        Timer timer = new Timer(1, e -> updateSpinner());
        timer.start();
    }

    private void updateSpinner() {
        angle += rotationSpeed;
        if (expanding) {
            arcLength += arcSpeed;
            rotationSpeed = 3;
            if (arcLength >= 270) {
                expanding = false;
            }
        } else {
            arcLength -= arcSpeed * 2;
            rotationSpeed = 6;
            if (arcLength <= 90) {
                expanding = true;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        drawSpinner(g2d, getWidth(), getHeight());
        repaint();
    }

    private void drawSpinner(Graphics2D g2d, int width, int height) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 140, 0));
        g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int size = Math.min(width, height) - 20;
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = size / 3;

        g2d.drawArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius,
                (int) angle, (int) arcLength);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Swing Spinner");
            frame.setLocationRelativeTo(null);
            SwingSpinner spinner = new SwingSpinner();
            frame.add(spinner);
            frame.setSize(200, 200);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
