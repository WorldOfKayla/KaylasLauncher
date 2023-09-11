package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.gui.components.Align;
import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.image.BufferedImage;

import javax.swing.SwingConstants;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;


public class ButtonStyle {
	public boolean visible = false;
	public String font;
	public String color;
	public float fontSize;
	public Align align;
	public BufferedImage texture;

	public ButtonStyle(String texture, String font, float fontSize, String color, boolean visible, Align align) {
		this.visible = visible;
		this.color = color;
		this.font = font;
		this.fontSize = fontSize;
		this.align = align;
		this.texture = ImageUtils.getLocalImage(texture);
	}

	public void apply(Button button) {
		button.setVisible(visible);
		button.setHorizontalAlignment(align == Align.LEFT ? SwingConstants.LEFT : align == Align.CENTER ? SwingConstants.CENTER : SwingConstants.RIGHT);
		button.setFont(FontUtils.getFont(font, fontSize));
		button.setForeground(hexToColor(color));
		int i = texture.getHeight() / 4;
		button.defaultTX = texture.getSubimage(0, 0, texture.getWidth(), i);
		button.rolloverTX = texture.getSubimage(0, i, texture.getWidth(), i);
		button.pressedTX = texture.getSubimage(0, i * 2, texture.getWidth(), i);
		button.lockedTX = texture.getSubimage(0, i * 3, texture.getWidth(), i);
	}
}