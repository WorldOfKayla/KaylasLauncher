package org.foxesworld.engine.gui.components.button;

import org.foxesworld.engine.gui.components.ComponentAttributes;
import org.foxesworld.engine.gui.components.ComponentFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class Button extends JButton implements MouseListener, MouseMotionListener {
    private Color hoverColor;
    private boolean entered = false;
    private boolean pressed = false;
    public BufferedImage defaultTX;
    public BufferedImage rolloverTX;
    public BufferedImage pressedTX;
    public BufferedImage lockedTX;
    private final ComponentFactory componentFactory;
    private final ComponentAttributes buttonAttributes;

    public Button(ComponentFactory componentFactory, String text) {
        this.componentFactory = componentFactory;
        this.buttonAttributes = componentFactory.getComponentAttribute();
        addMouseListener(this);
        addMouseMotionListener(this);
        setText(text);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(componentFactory.style.isOpaque());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public Button(ComponentFactory componentFactory, ImageIcon icon) {
        super();
        this.componentFactory = componentFactory;
        this.buttonAttributes = componentFactory.getComponentAttribute();
        addMouseListener(this);
        addMouseMotionListener(this);

        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(componentFactory.style.isOpaque());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        iconLabel.setVerticalAlignment(JLabel.CENTER);

        setLayout(new BorderLayout());
        add(iconLabel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        BufferedImage imageToDraw = defaultTX;

        if (!isEnabled()) {
            imageToDraw = lockedTX;
        } else if (pressed) {
            imageToDraw = pressedTX;
        } else if (entered) {
            g.setColor(this.hoverColor);
            imageToDraw = rolloverTX;
        }

        g.drawImage(imageToDraw, 0, 0, w, h, null);

        if (getText() != null && !getText().isEmpty()) {
            FontMetrics fm = g.getFontMetrics();
            int textX = (w - fm.stringWidth(getText())) / 2;
            int textY = (h + fm.getAscent()) / 2;
            if (isEnabled()) {
                g.setColor(entered ? this.hoverColor : getForeground());
            }
            g.drawString(getText(), textX, textY);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        entered = true;
        if (isEnabled()) {
            componentFactory.engine.getSOUND().playSound("button/buttonHover.ogg", false);
        }
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        entered = false;
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (isEnabled() && e.getButton() == MouseEvent.BUTTON1) {
            ButtonClick();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (pressed && e.getButton() == MouseEvent.BUTTON1) {
            pressed = false;
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    public void ButtonClick() {
        String sound;
        if (this.buttonAttributes.getComponentId().contains("back")) {
            sound = "buttonBack.ogg";
        } else if (this.buttonAttributes.getComponentId().contains("small")) {
            sound = "buttonClickSmall.ogg";
        } else {
            sound = "buttonClick.ogg";
        }

        componentFactory.engine.getSOUND().playSound("button/" + sound, false);
        pressed = true;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public void setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
    }
}
