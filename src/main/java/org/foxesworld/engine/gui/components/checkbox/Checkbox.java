package org.foxesworld.engine.gui.components.checkbox;

import org.foxesworld.engine.gui.components.Components;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.JCheckBox;

public class Checkbox extends JCheckBox {
    private static final long serialVersionUID = 1L;
    public BufferedImage defaultTX;
    public BufferedImage rolloverTX;
    public BufferedImage selectedTX;
    public BufferedImage selectedRolloverTX;
    private  Components components;

    public Checkbox(Components components, String string) {
        super(string);
        this.components = components;
        this.setOpaque(false);
        this.setFocusable(false);
        this.listener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void listener(final JCheckBox Checkbox) {
        this.addMouseListener(new MouseListener(){

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {
                boolean isSel = Checkbox.isSelected();
                if (isEnabled() && e.getButton() == MouseEvent.BUTTON1) {
                    if (isSel) {
                        components.engine.getSound().playSound("checkbox/checkboxOff2.ogg");
                    } else {
                        components.engine.getSound().playSound("checkbox/checkboxOn2.ogg");
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseClicked(MouseEvent e) {}
        });
    }
}

