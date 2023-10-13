package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.Serial;


public class Textfield extends JTextField {
	@Serial
	private static final long serialVersionUID = 1L;

	public BufferedImage texture;
	private String placeholder;

	public Textfield(String placeholder) {
		setOpaque(false);
		this.placeholder = placeholder;
		setText(placeholder);
		setForeground(UIManager.getColor("textInactiveText"));

		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if (getText().equals(placeholder)) {
					setText("");
					setForeground(UIManager.getColor("textText"));
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (getText().isEmpty()) {
					setText(placeholder);
					setForeground(UIManager.getColor("textInactiveText"));
				}
			}
		});
	}

	public String getActualText() {
		if (getText().equals(placeholder)) {
			return "";
		}
		return getText();
	}

	protected void paintComponent(Graphics maing) {
		Graphics2D g = (Graphics2D) maing.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(ImageUtils.genButton(getWidth(), getHeight(), texture), 0, 0, getWidth(), getHeight(), null);
		g.dispose();
		super.paintComponent(maing);
	}
}