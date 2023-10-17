package org.foxesworld.newengine.gui.components.label;

import org.foxesworld.newengine.gui.components.Components;
import org.foxesworld.newengine.gui.styles.StyleProvider;

import java.awt.Color;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;


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