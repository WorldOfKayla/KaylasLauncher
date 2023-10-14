package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class TextfieldStyle {
	public Color foregroundColor;
	public Color backgroundColor;
	public Color border;
	public int width;
	public int height;
	public String font;
	public float fontSize;
	public Color caretColor;

	public BufferedImage texture;

	public TextfieldStyle(String texture, int width, int height, String font, float fontSize, String color, String backgroundColor, String borderColor, String caretColor) {
		System.out.println(backgroundColor);
		this.foregroundColor = hexToColor(color);
		this.backgroundColor = hexToColor(backgroundColor);
		this.border = hexToColor(borderColor);
		this.caretColor = hexToColor(caretColor);
		this.width = width;
		this.height = height;
		this.font = font;
		this.fontSize = fontSize;
		this.texture = ImageUtils.getLocalImage(texture);
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setCaretColor(caretColor);

		//text.setBackground(backgroundColor);
		text.setForeground(foregroundColor);
		text.setBorder(null);
		//text.setOpaque(false);
		text.setFont(FontUtils.getFont(font, fontSize));
	}
}