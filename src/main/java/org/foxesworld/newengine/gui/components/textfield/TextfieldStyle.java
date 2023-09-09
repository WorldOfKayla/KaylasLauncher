package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.border.Border;

import static org.foxesworld.newengine.utils.FontUtils.getFont;

public class TextfieldStyle {
	public String fontName;
	public float fontSize = 1F;
	public Color textColor;
	public Color caretColor;
	public BufferedImage texture;
	public Border border;

	public TextfieldStyle(String texture, String fontName, float fontSize, Color textColor, Color caretColor) {
		this.fontName = fontName;
		this.fontSize = fontSize;
		this.textColor = textColor;
		this.caretColor = caretColor;
		this.texture = ImageUtils.getLocalImage(texture);
		//this.border = border;
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setFont(getFont(fontName, fontSize));
		text.setCaretColor(caretColor);
		text.setBackground(textColor);
		text.setForeground(textColor);
		text.setBorder(border);
	}
}