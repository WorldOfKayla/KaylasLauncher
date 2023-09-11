package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.gui.components.Align;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.SwingConstants;


public class ButtonStyle {
	public Color color;
	public boolean visible = false;
	public Align align;
	public BufferedImage texture;

	public ButtonStyle(String texture, Color color, boolean visible, Align align) {
		this.color = color;
		this.visible = visible;
		this.align = align;
		this.texture = ImageUtils.getLocalImage(texture);
	}

	public void apply(Button button) {
		button.setVisible(visible);
		button.setForeground(color);
		button.setHorizontalAlignment(align == Align.LEFT ? SwingConstants.LEFT : align == Align.CENTER ? SwingConstants.CENTER : SwingConstants.RIGHT);

		int i = texture.getHeight() / 4;
		button.defaultTX = texture.getSubimage(0, 0, texture.getWidth(), i);
		button.rolloverTX = texture.getSubimage(0, i, texture.getWidth(), i);
		button.pressedTX = texture.getSubimage(0, i * 2, texture.getWidth(), i);
		button.lockedTX = texture.getSubimage(0, i * 3, texture.getWidth(), i);
	}
}