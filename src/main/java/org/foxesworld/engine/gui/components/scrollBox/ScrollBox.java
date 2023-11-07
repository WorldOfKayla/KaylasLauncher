package org.foxesworld.engine.gui.components.scrollBox;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ScrollBox extends JComponent implements MouseListener, MouseMotionListener {

    private boolean loaded = false;
    private ComponentFactory componentFactory;
    private ScrollBoxListener scrollBoxListener;
    private static final long serialVersionUID = 1L;
    public String[] values;
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

    public ScrollBox(ComponentFactory componentFactory, String[] values, int y) {
        this.componentFactory = componentFactory;
        componentFactory.engine.getLOGGER().info("Updating ScrollBox " + values.toString());
        this.values = values;
        initialy = y;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.setFocusable(true);
        this.addFocusListener(new FocusAdapter(){

            @Override
            public void focusLost(FocusEvent e) {
                opened = false;
                hover = selected;
                componentFactory.engine.getFrame().getFrame().repaint();
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
            int righth = this.openedTX.getHeight() * (this.values.length + 1);
            int righty = initialy + this.openedTX.getHeight() - righth;
            if (this.getY() != righty || this.getHeight() != righth) {
                this.setLocation(this.getX(), righty);
                this.setSize(this.getWidth(), righth);
                y = this.getHeight();
                return;
            }
            for (int i = 0; i < this.values.length; ++i) {
                if (hover != i) {
                    g.drawImage(this.panelTX, 0, this.panelTX.getHeight() * i, this);
                } else {
                    g.drawImage(this.selectedTX, 0, this.panelTX.getHeight() * i, this);
                }
                g.drawString(this.values[i], 5, this.selectedTX.getHeight() * (i + 1) - g.getFontMetrics().getHeight() / 2);
                if (i != selected) continue;
                g.drawImage(this.point, 176, this.panelTX.getHeight() * i + 3, this);
            }
            g.drawString(this.values[selected], 5, this.selectedTX.getHeight() * (this.values.length + 1) - g.getFontMetrics().getHeight() / 2);
        } else if (entered) {
            int righth = this.openedTX.getHeight();
            if (this.getY() != initialy || this.getHeight() != righth) {
                this.setLocation(this.getX(), initialy);
                this.setSize(this.getWidth(), righth);
                return;
            }
            g.drawImage(ImageUtils.genButton(w, this.rolloverTX.getHeight(), this.rolloverTX), 0, 0, w, this.rolloverTX.getHeight(), null);
            g.drawString(this.values[selected], 5, this.rolloverTX.getHeight() - g.getFontMetrics().getHeight() / 2);
        } else {
            int righth = this.openedTX.getHeight();
            if (this.getY() != initialy || this.getHeight() != righth) {
                this.setLocation(this.getX(), initialy);
                this.setSize(this.getWidth(), righth);
                return;
            }
            g.drawImage(ImageUtils.genButton(w, this.defaultTX.getHeight(), this.defaultTX), 0, 0, w, this.defaultTX.getHeight(), null);
            g.drawString(this.values[selected], 5, this.rolloverTX.getHeight() - g.getFontMetrics().getHeight() / 2);
        }
        g.dispose();
        if(!loaded){
            this.scrollBoxListener.onScrollBoxCreated(selected);
            setLoaded(true);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() != 1) {
            return;
        }
        this.grabFocus();
        this.requestFocus();
        if (opened && y / this.openedTX.getHeight() < this.values.length) {
            selected = y / this.openedTX.getHeight();
            entered = ImageUtils.contains(x, y, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
        if (opened) {
            scrollBoxListener.onScrollBoxClose(this.selected);
            componentFactory.engine.getSOUND().playSound("scrollBox/scrollBoxOff.ogg");
        } else {
            scrollBoxListener.onScrollBoxOpen(this.selected);
            componentFactory.engine.getSOUND().playSound("scrollBox/scrollBoxOn.ogg");
        }
        boolean bl = opened = !opened;

        hover = selected;
        this.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (!opened) {
            componentFactory.engine.getSOUND().playSound("button/buttonHover.ogg");
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
        if (opened && y / this.openedTX.getHeight() < this.values.length) {
            if (hover != y / this.openedTX.getHeight()) {
                this.repaint();
            }
            hover = y / this.openedTX.getHeight();
        }
        this.repaint();
        if(this.opened) {
            scrollBoxListener.onServerHover(hover);
        }
    }

    public static int getSelectedIndex() {
        return selected;
    }

    public static int getHoverIndex() {
        return hover;
    }

    public String getSelected() {
        try {
            return this.values[selected];
        }
        catch (Exception e) {
            e.printStackTrace();
            return this.values[0];
        }
    }

    public boolean setSelectedIndex(int i) {
        if (this.values.length <= i) {
            return false;
        }
        selected = i;
        this.repaint();
        return true;
    }

    public void setValues(String[] values){
        this.values = values;
    }

    public static boolean isOpened() {
        return opened;
    }

    public void setScrollBoxListener(ScrollBoxListener scrollBoxListener) {
        this.scrollBoxListener = scrollBoxListener;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}