package org.foxesworld.newengine.gui.components.button;

import org.foxesworld.newengine.gui.components.Align;
import org.foxesworld.newengine.gui.components.Components;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.utils.FontUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import java.awt.image.BufferedImage;

import javax.swing.SwingConstants;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;


public class ButtonStyle {
	public boolean visible = true;
	public  int width;
	public int height;
	public String font;
	public String color;
	public float fontSize;
	public Align align;
	public BufferedImage texture;
	private  Components components;

	public ButtonStyle(Components components) {
		this.components = components;
		this.width = components.style.width;
		this.height = components.style.height;
		this.color = components.style.color;
		this.font = components.style.font;
		this.fontSize = components.style.fontSize;
		this.align = Align.valueOf(components.style.align);
		this.texture = ImageUtils.getLocalImage(components.style.texture);
	}

	public void apply(Button button) {
		button.setVisible(visible);
		button.setHorizontalAlignment(align == Align.LEFT ? SwingConstants.LEFT : align == Align.CENTER ? SwingConstants.CENTER : SwingConstants.RIGHT);
		button.setFont(components.appFrame.getFontUtils().getFont(font, fontSize));
		button.setForeground(hexToColor(color));
		int i = texture.getHeight() / 4;
		button.defaultTX = texture.getSubimage(0, 0, texture.getWidth(), i);
		button.rolloverTX = texture.getSubimage(0, i, texture.getWidth(), i);
		button.pressedTX = texture.getSubimage(0, i * 2, texture.getWidth(), i);
		button.lockedTX = texture.getSubimage(0, i * 3, texture.getWidth(), i);
	}
}