package org.foxesworld.newengine.gui.components.textfield;

import org.foxesworld.newengine.gui.styles.StyleProvider;
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

	public TextfieldStyle(StyleProvider.StyleAttributes styles) {
		this.foregroundColor = hexToColor(styles.color);
		this.backgroundColor = hexToColor(styles.background);
		this.border = hexToColor(styles.borderColor);
		this.caretColor = hexToColor(styles.caretColor);
		this.width = styles.width;
		this.height = styles.height;
		this.font = styles.font;
		this.fontSize = styles.fontSize;
		this.texture = ImageUtils.getLocalImage(styles.texture);
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setCaretColor(caretColor);
		text.setBackground(backgroundColor);
		text.setForeground(foregroundColor);
		text.setBorder(null);
		text.setFont(FontUtils.getFont(font, fontSize));
	}
}