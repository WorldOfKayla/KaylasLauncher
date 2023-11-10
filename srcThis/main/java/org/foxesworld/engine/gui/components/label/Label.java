package org.foxesworld.engine.gui.components.label;

import javax.swing.*;
import java.awt.*;


public class Label extends JLabel {
	private static final long serialVersionUID = 1L;

	public Label(String title) {
		setText(title);
		setOpaque(false);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
}