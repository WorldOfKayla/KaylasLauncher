package org.foxesworld.engine.gui.components.label;

import org.foxesworld.engine.gui.components.Components;

import java.awt.Color;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;


public class LabelStyle {
	public String fontName;
	public float fontSize;
	public Color idleColor;
	public Color activeColor;

	public LabelStyle(Components components) {
		this.fontName = components.style.font;
		this.fontSize = components.style.fontSize;
		this.idleColor = hexToColor(components.style.color);
		this.activeColor = hexToColor(components.style.color);
	}

	public void apply(Label label) {
		label.setForeground(activeColor);
	}
}