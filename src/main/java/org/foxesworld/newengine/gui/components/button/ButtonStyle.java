package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.Align;
import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.SwingConstants;


public class ButtonStyle {
	private APP app;
	public int x = 0;
	public int y = 0;
	public int w = 0;
	public int h = 0;
	public String fontName;
	public float fontSize = 1F;
	public Color color;
	public boolean visible = false;
	public Align align;
	public BufferedImage texture;

	public ButtonStyle(APP app, int x, int y, int w, int h, String fontName, String texture, float fontSize, Color color, boolean visible, Align align) {
		this.app = app;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.fontName = fontName;
		this.fontSize = fontSize;
		this.color = color;
		this.visible = visible;
		this.align = align;
		this.texture = ImageUtils.getLocalImage(texture);
	}

	public void apply(Button button) {
		button.setVisible(visible);
		button.setBounds(x, y, w, h);
		button.setForeground(color);
		button.setFont(FontUtils.getFont(fontName, fontSize));
		button.setHorizontalAlignment(align == Align.LEFT ? SwingConstants.LEFT : align == Align.CENTER ? SwingConstants.CENTER : SwingConstants.RIGHT);

		int i = texture.getHeight() / 4;
		button.defaultTX = texture.getSubimage(0, 0, texture.getWidth(), i);
		button.rolloverTX = texture.getSubimage(0, i, texture.getWidth(), i);
		button.pressedTX = texture.getSubimage(0, i * 2, texture.getWidth(), i);
		button.lockedTX = texture.getSubimage(0, i * 3, texture.getWidth(), i);
	}
}