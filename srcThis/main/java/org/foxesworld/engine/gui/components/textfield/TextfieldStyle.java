package org.foxesworld.engine.gui.components.textfield;

import org.foxesworld.engine.gui.components.ComponentFactory;
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
	private ComponentFactory componentFactory;

	public TextfieldStyle(ComponentFactory componentFactory) {
		this.componentFactory = componentFactory;
		this.foregroundColor = hexToColor(componentFactory.style.color);
		this.backgroundColor = hexToColor(componentFactory.style.background);
		this.border = hexToColor(componentFactory.style.borderColor);
		this.caretColor = hexToColor(componentFactory.style.caretColor);
		this.width = componentFactory.style.width;
		this.height = componentFactory.style.height;
		this.font = componentFactory.style.font;
		this.fontSize = componentFactory.style.fontSize;
		this.texture = ImageUtils.getLocalImage(componentFactory.style.texture);
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setCaretColor(caretColor);
		text.setBackground(backgroundColor);
		text.setForeground(foregroundColor);
		text.setBorder(null);
		text.setFont(componentFactory.engine.getFONTUTILS().getFont(font, fontSize));
	}
}