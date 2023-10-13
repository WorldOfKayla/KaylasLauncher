package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.border.Border;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class TextfieldStyle {
	public Color textColor;
	public int width;
	public int height;
	public String font;
	public float fontSize;
	public Color caretColor;

	public Color border;
	public BufferedImage texture;

	public TextfieldStyle(String texture, int width, int height, String font, float fontSize, String textColor, String borderColor, Color caretColor) {
		this.textColor = hexToColor(textColor);
		this.border = hexToColor(borderColor);
		this.width = width;
		this.height = height;
		this.font = font;
		this.fontSize = fontSize;
		this.caretColor = caretColor;
		this.texture = ImageUtils.getLocalImage(texture);
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setCaretColor(caretColor);
		text.setBackground(textColor);
		text.setForeground(textColor);
		text.setFont(FontUtils.getFont(font, fontSize));
	}
}