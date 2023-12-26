package org.foxesworld.engine.gui.components.dropBox;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class DropBox extends JComponent implements MouseListener, MouseMotionListener {
    private boolean loaded = false;
    private final ComponentFactory componentFactory;
    private DropBoxListener dropBoxListener;
    private int previousHover = -1;

    private String[] values;
    private static int initialY = 0;
    private static boolean entered = false;
    private static boolean opened = false;
    private static int x = 0;
    private static int y = 0;
    private static int selected;
    private static int hover;

    BufferedImage defaultTX;
    BufferedImage openedTX;
    BufferedImage rolloverTX;
    BufferedImage selectedTX;
    BufferedImage panelTX;
    BufferedImage point;

    public DropBox(ComponentFactory componentFactory, String[] values, int initialY) {
        this.componentFactory = componentFactory;
        this.values = values;
        DropBox.initialY = initialY;

        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                opened = false;
                hover = selected;
                componentFactory.engine.getFrame().getFrame().repaint();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics gmain) {
        Graphics2D g = (Graphics2D) gmain;
        int w = getWidth();
        g.setColor(Color.WHITE);

        if (opened) {
            drawOpenedState(g, w);
        } else if (entered) {
            drawRolloverState(g, w);
        } else {
            drawDefaultState(g, w);
        }

        g.dispose();

        if (!loaded) {
            dropBoxListener.onScrollBoxCreated(selected);
            setLoaded(true);
        }
    }

    private void drawOpenedState(Graphics2D g, int w) {
        g.drawImage(ImageUtils.genButton(w, openedTX.getHeight(), openedTX), 0, getHeight() - openedTX.getHeight(), w, openedTX.getHeight(), null);

        int rightHeight = openedTX.getHeight() * (values.length + 1);
        int rightY = initialY + openedTX.getHeight() - rightHeight;

        if (getY() != rightY || getHeight() != rightHeight) {
            setLocation(getX(), rightY);
            setSize(getWidth(), rightHeight);
            y = getHeight();
            return;
        }

        for (int i = 0; i < values.length; ++i) {
            drawPanel(g, i);
            if (i == selected) {
                g.drawImage(point, 176, panelTX.getHeight() * i + 10, this);
            }
        }
        g.drawString(values[selected], 10, selectedTX.getHeight() * (values.length + 1) - g.getFontMetrics().getHeight() / 2 - 5);
    }

    private void drawRolloverState(Graphics2D g, int w) {
        int rightHeight = rolloverTX.getHeight();
        if (getY() != initialY || getHeight() != rightHeight) {
            setLocation(getX(), initialY);
            setSize(getWidth(), rightHeight);
            return;
        }

        g.drawImage(ImageUtils.genButton(w, rolloverTX.getHeight(), rolloverTX), 0, 0, w, rolloverTX.getHeight(), null);
        g.drawString(values[selected], 10, rolloverTX.getHeight() - g.getFontMetrics().getHeight() / 2 - 5);
    }

    private void drawDefaultState(Graphics2D g, int w) {
        int rightHeight = defaultTX.getHeight();
        if (getY() != initialY || getHeight() != rightHeight) {
            setLocation(getX(), initialY);
            setSize(getWidth(), rightHeight);
            return;
        }

        g.drawImage(ImageUtils.genButton(w, defaultTX.getHeight(), defaultTX), 0, 0, w, defaultTX.getHeight(), null);
        g.drawString(values[selected], 10, rolloverTX.getHeight() - g.getFontMetrics().getHeight() / 2 - 5);
    }

    private void drawPanel(Graphics2D g, int i) {
        if (hover != i) {
            g.drawImage(panelTX, 0, panelTX.getHeight() * i, this);
        } else {
            g.drawImage(selectedTX, 0, panelTX.getHeight() * i, this);
        }
        g.drawString(values[i], 5, selectedTX.getHeight() * (i + 1) - g.getFontMetrics().getHeight() / 2 - 5);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        grabFocus();

        if (opened && y / openedTX.getHeight() < values.length) {
            selected = y / openedTX.getHeight();
            entered = ImageUtils.contains(x, y, getX(), getY(), getWidth(), getHeight());
        }

        if (opened) {
            dropBoxListener.onScrollBoxClose(selected);
            componentFactory.engine.getSOUND().playSound("dropBox/dropBoxOpen.ogg", false);
        } else {
            dropBoxListener.onScrollBoxOpen(selected);
            componentFactory.engine.getSOUND().playSound("dropBox/dropBoxClose.ogg", false);
        }

        opened = !opened;
        hover = selected;
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (!opened) {
            componentFactory.engine.getSOUND().playSound("button/buttonHover.ogg", false);
        }
        entered = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        entered = false;
        repaint();
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
        int newHover = opened ? (y / openedTX.getHeight()) : -1;
        if (newHover >= 0 && newHover < values.length && newHover != previousHover) {
            if (opened) {
                dropBoxListener.onServerHover(newHover);
            }
            previousHover = newHover;
            repaint();
            hover = y / openedTX.getHeight();
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
            return values[selected];
        } catch (Exception e) {
            e.printStackTrace();
            return values[0];
        }
    }

    public boolean setSelectedIndex(int i) {
        if (values.length <= i) {
            return false;
        }
        selected = i;
        repaint();
        return true;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public static boolean isOpened() {
        return opened;
    }

    public void setScrollBoxListener(DropBoxListener dropBoxListener) {
        this.dropBoxListener = dropBoxListener;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
