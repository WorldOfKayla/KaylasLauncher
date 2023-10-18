package org.foxesworld.engine.gui.components.checkbox;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.JCheckBox;

public class Checkbox extends JCheckBox {
    private static final long serialVersionUID = 1L;
    private Integer pressedNum = 0;
    public BufferedImage defaultTX;
    public BufferedImage rolloverTX;
    public BufferedImage selectedTX;
    public BufferedImage selectedRolloverTX;
    public boolean isSel;

    public Checkbox(String string) {
        super(string);
        this.setOpaque(false);
        this.setFocusable(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void checkbox_listener(final JCheckBox Checkbox2) {
        this.addMouseListener(new MouseListener(){

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                boolean isSel = Checkbox2.isSelected();
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });
    }
}

