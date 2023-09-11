package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;


public class Textfield extends JTextField {
	@Serial
	private static final long serialVersionUID = 1L;

	public BufferedImage texture;

	public Textfield() {
		setOpaque(false);
	}

	protected void paintComponent(Graphics maing) {
		Graphics2D g = (Graphics2D) maing.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(ImageUtils.genButton(getWidth(), getHeight(), texture), 0, 0, getWidth(), getHeight(), null);
		g.dispose();
		super.paintComponent(maing);
	}
}