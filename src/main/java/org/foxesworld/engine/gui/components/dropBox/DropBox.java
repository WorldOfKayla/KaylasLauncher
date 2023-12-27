package org.foxesworld.engine.gui.components.dropBox;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class DropBox extends JComponent implements MouseListener, MouseMotionListener {

    private enum State {CLOSED, OPENED, ROLLOVER}
    private Color color, hoverColor;
    private boolean loaded = false;
    private final ComponentFactory componentFactory;
    private DropBoxListener dropBoxListener;
    private int previousHover = -1;
    private String[] values;
    private int initialY;
    private State state = State.CLOSED;
    private int x = 0;
    private int y = 0;
    private int selected;
    private int hover;
    private boolean entered;
    private BufferedImage defaultTX;
    private BufferedImage openedTX;
    private BufferedImage rolloverTX;
    private BufferedImage selectedTX;
    private BufferedImage panelTX;
    private BufferedImage point;

    public DropBox(ComponentFactory componentFactory, String[] values, int initialY) {
        this.componentFactory = componentFactory;
        this.values = values;
        this.initialY = initialY;

        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                state = State.CLOSED;
                hover = selected;
                componentFactory.engine.getFrame().repaint();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics gmain) {
        Graphics2D g = (Graphics2D) gmain;
        int w = getWidth();
        g.setColor(hexToColor(componentFactory.style.getColor()));

        switch (state) {
            case OPENED -> drawOpenedState(g, w);
            case ROLLOVER -> drawRolloverState(g, w);
            default -> drawDefaultState(g, w);
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

        // Draw the button with rollover effect
        g.drawImage(ImageUtils.genButton(w, rolloverTX.getHeight(), rolloverTX), 0, 0, w, rolloverTX.getHeight(), null);

        // Draw the text
        g.setColor(hoverColor); // Set the color for HOVER TEXT
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

        if (state == State.OPENED && y / openedTX.getHeight() < values.length) {
            selected = y / openedTX.getHeight();
            entered = ImageUtils.contains(x, y, getX(), getY(), getWidth(), getHeight());
        }

        if (state == State.OPENED) {
            dropBoxListener.onScrollBoxClose(selected);
            componentFactory.engine.getSOUND().playSound("dropBox/dropBoxOpen.ogg", false);
        } else {
            dropBoxListener.onScrollBoxOpen(selected);
            componentFactory.engine.getSOUND().playSound("dropBox/dropBoxClose.ogg", false);
        }

        state = (state == State.OPENED) ? State.CLOSED : State.OPENED;
        hover = selected;
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (state != State.OPENED) {
            componentFactory.engine.getSOUND().playSound("button/buttonHover.ogg", false);
        }
        state = State.ROLLOVER;
        entered = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        entered = false;
        state = State.CLOSED;
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        grabFocus();
        handleMouseClick(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        handleMouseClick(e);
    }

    private void handleMouseClick(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
    }

        @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        y = e.getY();
        x = e.getX();
        int newHover = (state == State.OPENED) ? (y / openedTX.getHeight()) : -1;
        if (newHover >= 0 && newHover < values.length && newHover != previousHover) {
            if (state == State.OPENED) {
                dropBoxListener.onServerHover(newHover);
            }
            previousHover = newHover;
            repaint();
            hover = y / openedTX.getHeight();
        }
    }

    public int getSelectedIndex() {
        return selected;
    }

    public int getHoverIndex() {
        return hover;
    }

    public String getSelected() {
        try {
            return values[selected];
        } catch (ArrayIndexOutOfBoundsException e) {
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

    public boolean isOpened() {
        return state == State.OPENED;
    }

    public void setScrollBoxListener(DropBoxListener dropBoxListener) {
        this.dropBoxListener = dropBoxListener;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void setDefaultTX(BufferedImage defaultTX) {
        this.defaultTX = defaultTX;
    }

    public void setOpenedTX(BufferedImage openedTX) {
        this.openedTX = openedTX;
    }

    public void setRolloverTX(BufferedImage rolloverTX) {
        this.rolloverTX = rolloverTX;
    }

    public void setSelectedTX(BufferedImage selectedTX) {
        this.selectedTX = selectedTX;
    }

    public void setPanelTX(BufferedImage panelTX) {
        this.panelTX = panelTX;
    }

    public void setPoint(BufferedImage point) {
        this.point = point;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
    }
}