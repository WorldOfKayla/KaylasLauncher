package org.foxesworld.engine.gui.components.label;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;


public class Label extends JLabel {
	@Serial
	private static final long serialVersionUID = 1L;

	public Label(String title) {
		setText(title);
		setOpaque(false);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
}