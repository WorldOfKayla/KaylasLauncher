package org.foxesworld.engine.gui.components.slider;

import org.foxesworld.engine.gui.components.ComponentFactory;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class Slider extends JSlider implements MouseListener, MouseMotionListener {
    private ComponentFactory componentFactory;

    public Slider(ComponentFactory componentFactory){
        super(componentFactory.getComponentAttribute().getMinValue(), componentFactory.getComponentAttribute().getMaxValue());
        this.componentFactory = componentFactory;
        addMouseListener(this);
        addMouseMotionListener(this);

        this.addChangeListener(e ->componentFactory.engine.getCONFIG().setVolume(this.getValue()));
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
