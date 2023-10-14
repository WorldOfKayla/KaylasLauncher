package org.foxesworld.newengine.gui.components;

import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.attributes.ComponentAttributes;
import org.foxesworld.newengine.gui.components.button.ButtonStyle;
import org.foxesworld.newengine.gui.components.button.StyledButton;
import org.foxesworld.newengine.gui.components.checkbox.CheckboxStyle;
import org.foxesworld.newengine.gui.components.checkbox.StyledCheckbox;
import org.foxesworld.newengine.gui.components.label.LabelStyle;
import org.foxesworld.newengine.gui.components.label.StyledLabel;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyle;
import org.foxesworld.newengine.gui.components.progressBar.StyledProgressBar;
import org.foxesworld.newengine.gui.components.textfield.StyledTextfield;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;

public class Components {

    private AppFrame appFrame;
    private LanguageProvier LANG;
    private TextfieldStyle textfieldStyle;
    private ProgressBarStyle progressBarStyle;
    private LabelStyle labelStyle;
    private ButtonStyle buttonStyle;

    private CheckboxStyle checkboxStyle;

    public Components(AppFrame appFrame){
        this.appFrame = appFrame;
        this.LANG = appFrame.getApp().getLANG();
    }

    public JComponent createComponent(ComponentAttributes componentAttributes, String componentType) {
        StyleProvider.StyleAttributes style = appFrame.getElementStyles().get(componentType).get(componentAttributes.componentStyle);
        switch (componentType) {

            case "progressBar" -> {
                progressBarStyle = new ProgressBarStyle(style);
                StyledProgressBar progressBar = new StyledProgressBar(progressBarStyle);
                progressBar.setName(componentAttributes.componentId);
                progressBar.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                return progressBar;
            }

            case "label" -> {
                labelStyle = new LabelStyle(style);
                StyledLabel label = new StyledLabel(LANG.getString(componentAttributes.localeKey), labelStyle);
                if(componentAttributes.imageIcon != null) {
                    label.setIcon(new ImageIcon(ImageUtils.getScaledImage(ImageUtils.loadImage(componentAttributes.imageIcon), componentAttributes.iconWidth, componentAttributes.iconHeight)));
                }
                labelStyle.apply(label);
                label.setName(componentAttributes.componentId);
                label.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                return label;
            }

            case "checkBox" -> {
                checkboxStyle = new CheckboxStyle(style);
                StyledCheckbox checkbox = new StyledCheckbox(LANG.getString(componentAttributes.localeKey), checkboxStyle);
                checkbox.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                checkbox.setName(componentAttributes.localeKey);
                return checkbox;
            }

            case "textField" -> {
                textfieldStyle = new TextfieldStyle(style);
                StyledTextfield textfield = new StyledTextfield(LANG.getString(componentAttributes.localeKey), textfieldStyle);
                textfieldStyle.apply(textfield);
                textfield.setName(componentAttributes.componentId);
                textfield.setBounds(componentAttributes.xPos, componentAttributes.yPos, textfieldStyle.width, textfieldStyle.height);
                textfield.setActionCommand(componentAttributes.componentId);
                textfield.addActionListener(appFrame);
                return textfield;
            }

            case "button" -> {
                buttonStyle = new ButtonStyle(style);
                StyledButton button = new StyledButton(LANG.getString(componentAttributes.localeKey), buttonStyle);
                buttonStyle.apply(button);
                button.setName(componentAttributes.componentId);
                button.setActionCommand(componentAttributes.componentId);
                button.setBounds(componentAttributes.xPos, componentAttributes.yPos, buttonStyle.width, buttonStyle.height);
                button.addActionListener(appFrame);
                return button;
            }

            default -> throw new IllegalArgumentException("Unsupported component type: " + componentType);
        }
    }
}
