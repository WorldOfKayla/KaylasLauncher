package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.border.Border;

public class TextfieldStyle {
	public Color textColor;
	public Color caretColor;
	public BufferedImage texture;
	public Border border;

	public TextfieldStyle(String texture, Color textColor, Color caretColor) {
		this.textColor = textColor;
		this.caretColor = caretColor;
		this.texture = ImageUtils.getLocalImage(texture);
		//this.border = border;
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setCaretColor(caretColor);
		text.setBackground(textColor);
		text.setForeground(textColor);
		text.setBorder(border);
	}
}