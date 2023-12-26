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
		this.foregroundColor = hexToColor(componentFactory.style.getColor());
		this.backgroundColor = hexToColor(componentFactory.style.getBackground());
		this.border = hexToColor(componentFactory.style.getBorderColor());
		this.caretColor = hexToColor(componentFactory.style.getCaretColor());
		this.width = componentFactory.style.getWidth();
		this.height = componentFactory.style.getHeight();
		this.font = componentFactory.style.getFont();
		this.fontSize = componentFactory.style.getFontSize();
		this.texture = ImageUtils.getLocalImage(componentFactory.style.getTexture());
	}

	public void apply(Textfield text) {
		text.texture = texture;
		text.setPaddingX(componentFactory.style.getPaddingX());
		text.setPaddingY(componentFactory.style.getPaddingY());
		text.setCaretColor(caretColor);
		text.setBackground(backgroundColor);
		text.setForeground(foregroundColor);
		text.setBorder(null);
		text.setFont(componentFactory.engine.getFONTUTILS().getFont(font, fontSize));
	}
}