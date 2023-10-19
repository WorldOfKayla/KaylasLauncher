package org.foxesworld.engine.gui.components.scrollBox;

import org.foxesworld.engine.gui.components.Components;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ScrollBox extends JComponent implements MouseListener, MouseMotionListener {

    private Components components;

    private static final long serialVersionUID = 1L;
    public final String[] elements;
    public static int initialy = 0;
    private static boolean entered = false;
    private static boolean opened = false;
    private static int x = 0;
    private static int y = 0;
    private static int selected;
    private static int hover;
    public BufferedImage defaultTX;
    public BufferedImage openedTX;
    public BufferedImage rolloverTX;
    public BufferedImage selectedTX;
    public BufferedImage panelTX;
    public BufferedImage point;

    public ScrollBox(Components components, String[] elements, int y) {
        this.components = components;
        components.appFrame.getLOGGER().info("Updating combobox " + elements.toString());
        this.elements = elements;
        initialy = y;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.setFocusable(true);
        this.addFocusListener(new FocusAdapter(){

            @Override
            public void focusLost(FocusEvent e) {
                opened = false;
                hover = selected;
                components.appFrame.getFrame().getFrame().repaint();
                ScrollBox.this.repaint();
            }
        });
    }

    @Override
    public void paintComponent(Graphics gmain) {
        Graphics2D g = (Graphics2D)gmain;
        int w = this.getWidth();
        g.setColor(Color.WHITE);
        if (opened) {
            g.drawImage(ImageUtils.genButton(w, this.openedTX.getHeight(), this.openedTX), 0, this.getHeight() - this.openedTX.getHeight(), w, this.openedTX.getHeight(), null);
            int righth = this.openedTX.getHeight() * (this.elements.length + 1);
            int righty = initialy + this.openedTX.getHeight() - righth;
            if (this.getY() != righty || this.getHeight() != righth) {
                this.setLocation(this.getX(), righty);
                this.setSize(this.getWidth(), righth);
                y = this.getHeight();
                return;
            }
            for (int i = 0; i < this.elements.length; ++i) {
                if (hover != i) {
                    g.drawImage(this.panelTX, 0, this.panelTX.getHeight() * i, this);
                } else {
                    g.drawImage(this.selectedTX, 0, this.panelTX.getHeight() * i, this);
                }
                g.drawString(this.elements[i], 5, this.selectedTX.getHeight() * (i + 1) - g.getFontMetrics().getHeight() / 2);
                if (i != selected) continue;
                g.drawImage(this.point, 176, this.panelTX.getHeight() * i + 3, this);
            }
            g.drawString(this.elements[selected], 5, this.selectedTX.getHeight() * (this.elements.length + 1) - g.getFontMetrics().getHeight() / 2);
        } else if (entered) {
            int righth = this.openedTX.getHeight();
            if (this.getY() != initialy || this.getHeight() != righth) {
                this.setLocation(this.getX(), initialy);
                this.setSize(this.getWidth(), righth);
                return;
            }
            g.drawImage(ImageUtils.genButton(w, this.rolloverTX.getHeight(), this.rolloverTX), 0, 0, w, this.rolloverTX.getHeight(), null);
            g.drawString(this.elements[selected], 5, this.rolloverTX.getHeight() - g.getFontMetrics().getHeight() / 2);
        } else {
            int righth = this.openedTX.getHeight();
            if (this.getY() != initialy || this.getHeight() != righth) {
                this.setLocation(this.getX(), initialy);
                this.setSize(this.getWidth(), righth);
                return;
            }
            g.drawImage(ImageUtils.genButton(w, this.defaultTX.getHeight(), this.defaultTX), 0, 0, w, this.defaultTX.getHeight(), null);
            g.drawString(this.elements[selected], 5, this.rolloverTX.getHeight() - g.getFontMetrics().getHeight() / 2);
        }
        g.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() != 1) {
            return;
        }
        this.grabFocus();
        this.requestFocus();
        if (opened && y / this.openedTX.getHeight() < this.elements.length) {
            selected = y / this.openedTX.getHeight();
            entered = ImageUtils.contains(x, y, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
        if (opened) {
            components.appFrame.getSound().playSound("scrollBox/scrollBoxOff.ogg");
        } else {
            components.appFrame.getSound().playSound("scrollBox/scrollBoxOn.ogg");
        }
        boolean bl = opened = !opened;

        hover = selected;
        this.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (!opened) {
            components.appFrame.getSound().playSound("button/buttonHover.ogg");
        }
        entered = true;
        this.repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        entered = false;
        this.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        y = e.getY();
        x = e.getX();
        if (opened && y / this.openedTX.getHeight() < this.elements.length) {
            if (hover != y / this.openedTX.getHeight()) {
                this.repaint();
            }
            hover = y / this.openedTX.getHeight();
        }
        this.repaint();
        //Frame.lore.setText(Settings.servers[hover].split("& ")[5]);
    }

    public static int getSelectedIndex() {
        return selected;
    }

    public static int getHoverIndex() {
        return hover;
    }

    public String getSelected() {
        try {
            return this.elements[selected];
        }
        catch (Exception e) {
            e.printStackTrace();
            return this.elements[0];
        }
    }

    public boolean setSelectedIndex(int i) {
        if (this.elements.length <= i) {
            return false;
        }
        selected = i;
        this.repaint();
        return true;
    }

    public static boolean isOpened() {
        return opened;
    }
}