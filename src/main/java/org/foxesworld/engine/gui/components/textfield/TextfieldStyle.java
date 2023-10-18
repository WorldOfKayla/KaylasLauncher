package org.foxesworld.engine.gui.components.textfield;

import org.foxesworld.engine.gui.components.Components;
import org.foxesworld.engine.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

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
	private  Components components;

	public TextfieldStyle(Components components) {
		this.components = components;
		this.foregroundColor = hexToColor(components.style.color);
		this.backgroundColor = hexToColor(components.style.background);
		this.border = hexToColor(components.style.borderColor);
		this.caretColor = hexToColor(components.style.caretColor);
		this.width = components.style.width;
		this.height = components.style.height;
		this.font = components.style.font;
		this.fontSize = components.style.fontSize;
		this.texture = ImageUtils.getLocalImage(components.style.texture);
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setCaretColor(caretColor);
		text.setBackground(backgroundColor);
		text.setForeground(foregroundColor);
		text.setBorder(null);
		text.setFont(components.appFrame.getFontUtils().getFont(font, fontSize));
	}
}