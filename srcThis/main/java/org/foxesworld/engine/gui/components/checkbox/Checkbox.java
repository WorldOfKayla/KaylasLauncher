package org.foxesworld.engine.gui.components.checkbox;

import org.foxesworld.engine.gui.components.ComponentFactory;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.JCheckBox;

public class Checkbox extends JCheckBox {
    public BufferedImage defaultTX;
    public BufferedImage rolloverTX;
    public BufferedImage selectedTX;
    public BufferedImage selectedRolloverTX;
    private ComponentFactory componentFactory;

    public Checkbox(ComponentFactory componentFactory, String string) {
        super(string);
        this.componentFactory = componentFactory;
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
                        componentFactory.engine.getSOUND().playSound("checkbox/checkboxOn.ogg");
                    } else {
                        componentFactory.engine.getSOUND().playSound("checkbox/checkboxOff.ogg");
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

