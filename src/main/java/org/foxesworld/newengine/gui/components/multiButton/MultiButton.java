package org.foxesworld.newengine.gui.components.multiButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class MultiButton extends JButton implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    public BufferedImage img1 = (BufferedImage) this.createImage(1, 1);
    public BufferedImage img2 = (BufferedImage) this.createImage(1, 1);
    public BufferedImage img3 = (BufferedImage) this.createImage(1, 1);
    private boolean entered = false;
    private boolean pressed = false;

    public MultiButton() {
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setFocusPainted(false);
        this.setOpaque(false);
        this.setFocusable(false);
    }

    @Override
    protected void paintComponent(Graphics maing) {
        Graphics2D g = (Graphics2D) maing.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (this.entered && !this.pressed) {
            g.drawImage(this.img2, 0, 0, this.getWidth(), this.getHeight(), null);
        }
        if (!this.entered) {
            g.drawImage(this.img1, 0, 0, this.getWidth(), this.getHeight(), null);
        }
        if (this.pressed && this.entered) {
            this.entered = false;
            g.drawImage(this.img3, 0, 0, this.getWidth(), this.getHeight(), null);
            this.pressed = false;
        }
        g.dispose();
        super.paintComponent(maing);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        //Click
        this.pressed = !this.pressed;
        this.repaint();
        this.revalidate();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //Hover
        this.entered = true;
        this.repaint();
        this.revalidate();
        ;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        this.entered = false;
        this.repaint();
        this.revalidate();
    }
}

