package org.foxesworld.newengine.gui.components.label;

import org.foxesworld.newengine.utils.FontUtils;

import java.awt.Color;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;


public class LabelStyle {
	public String fontName;
	public float fontSize;
	public Color idleColor;
	public Color activeColor;

	public LabelStyle(String fontName, float fontSize, String idleColor, String activeColor) {
		this.fontName = fontName;
		this.fontSize = fontSize;
		this.idleColor = hexToColor(idleColor);
		this.activeColor = hexToColor(activeColor);
	}

	public void apply(Label label) {
		label.setFont(FontUtils.getFont(fontName, fontSize));
		label.setForeground(activeColor);
	}
}