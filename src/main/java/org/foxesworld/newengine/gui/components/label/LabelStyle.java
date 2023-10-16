package org.foxesworld.newengine.gui.components.label;

import org.foxesworld.newengine.gui.styles.StyleProvider;

import java.awt.Color;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;


public class LabelStyle {
	public String fontName;
	public float fontSize;
	public Color idleColor;
	public Color activeColor;

	public LabelStyle(StyleProvider.StyleAttributes styles) {
		this.fontName = styles.font;
		this.fontSize = styles.fontSize;
		this.idleColor = hexToColor(styles.color);
		this.activeColor = hexToColor(styles.color);
	}

	public void apply(Label label) {
		label.setForeground(activeColor);
	}
}