package org.foxesworld.engine.gui.components.button;

import org.foxesworld.engine.gui.components.ComponentFactory;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;


public class ButtonStyle {
	public boolean visible = true;
	public  int width;
	public int height;
	public String font;
	public String color;
	public float fontSize;
	public ComponentFactory.Align align;
	public BufferedImage texture;
	private ComponentFactory componentFactory;
	public ButtonStyle(ComponentFactory componentFactory) {
		this.componentFactory = componentFactory;
		this.width = componentFactory.style.getWidth();
		this.height = componentFactory.style.getHeight();
		this.color = componentFactory.style.getColor();
		this.font = componentFactory.style.getFont();
		this.fontSize = componentFactory.style.getFontSize();
		this.align = ComponentFactory.Align.valueOf(componentFactory.style.getAlign());
		this.texture = ImageUtils.getLocalImage(componentFactory.style.getTexture());
	}
	public void apply(Button button) {
		button.setVisible(visible);
		button.setHorizontalAlignment(align == ComponentFactory.Align.LEFT ? SwingConstants.LEFT : align == ComponentFactory.Align.CENTER ? SwingConstants.CENTER : SwingConstants.RIGHT);
		button.setFont(componentFactory.engine.getFONTUTILS().getFont(font, fontSize));
		button.setHoverColor(hexToColor(this.componentFactory.style.getHoverColor()));
		button.setForeground(hexToColor(color));
		int i = texture.getHeight() / 4;
		button.defaultTX = texture.getSubimage(0, 0, texture.getWidth(), i);
		button.rolloverTX = texture.getSubimage(0, i, texture.getWidth(), i);
		button.pressedTX = texture.getSubimage(0, i * 2, texture.getWidth(), i);
		button.lockedTX = texture.getSubimage(0, i * 3, texture.getWidth(), i);
	}
}