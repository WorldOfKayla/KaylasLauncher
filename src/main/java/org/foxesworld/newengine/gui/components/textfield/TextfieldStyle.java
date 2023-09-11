package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.border.Border;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class TextfieldStyle {
	public Color textColor;
	public String font;
	public float fontSize;
	public Color caretColor;
	public BufferedImage texture;

	public TextfieldStyle(String texture, String font, float fontSize, String textColor, Color caretColor) {
		this.textColor = hexToColor(textColor);
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