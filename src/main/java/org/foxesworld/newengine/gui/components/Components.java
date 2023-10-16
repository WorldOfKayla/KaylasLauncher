package org.foxesworld.newengine.gui.components;

import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.attributes.ComponentAttributes;
import org.foxesworld.newengine.gui.components.button.Button;
import org.foxesworld.newengine.gui.components.button.ButtonStyle;
import org.foxesworld.newengine.gui.components.checkbox.Checkbox;
import org.foxesworld.newengine.gui.components.checkbox.CheckboxStyle;
import org.foxesworld.newengine.gui.components.label.Label;
import org.foxesworld.newengine.gui.components.label.LabelStyle;
import org.foxesworld.newengine.gui.components.multiButton.MultiButton;
import org.foxesworld.newengine.gui.components.multiButton.MultiButtonStyle;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyle;
import org.foxesworld.newengine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.newengine.gui.components.textfield.Textfield;
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
    private MultiButtonStyle multiButtonStyle;

    public Components(AppFrame appFrame){
        this.appFrame = appFrame;
        this.LANG = appFrame.getApp().getLANG();
    }

    public JComponent createComponent(ComponentAttributes componentAttributes, String componentType) {
        StyleProvider.StyleAttributes style = null;
        if(appFrame.getElementStyles().get(componentType)!=null) {
            style = appFrame.getElementStyles().get(componentType).get(componentAttributes.componentStyle);
        }
        switch (componentType) {

            case "progressBar" -> {
                progressBarStyle = new ProgressBarStyle(style);
                JProgressBar progressBar = new JProgressBar();
                progressBarStyle.apply(progressBar);
                progressBar.setName(componentAttributes.componentId);
                progressBar.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                return progressBar;
            }

            case "label" -> {
                labelStyle = new LabelStyle(style);
                Label label = new Label(LANG.getString(componentAttributes.localeKey));
                labelStyle.apply(label);
                if(componentAttributes.imageIcon != null) {
                    label.setIcon(new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(componentAttributes.imageIcon), componentAttributes.iconWidth, componentAttributes.iconHeight)));
                }
                labelStyle.apply(label);
                label.setName(componentAttributes.componentId);
                label.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                return label;
            }

            case "checkBox" -> {
                checkboxStyle = new CheckboxStyle(style);
                Checkbox checkbox = new Checkbox(LANG.getString(componentAttributes.localeKey));
                checkboxStyle.apply(checkbox);
                checkbox.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                checkbox.setName(componentAttributes.localeKey);
                return checkbox;
            }

            case "textField" -> {
                textfieldStyle = new TextfieldStyle(style);
                Textfield textfield = new Textfield(LANG.getString(componentAttributes.localeKey));
                textfieldStyle.apply(textfield);
                textfield.setName(componentAttributes.componentId);
                textfield.setBounds(componentAttributes.xPos, componentAttributes.yPos, textfieldStyle.width, textfieldStyle.height);
                textfield.setActionCommand(componentAttributes.componentId);
                textfield.addActionListener(appFrame);
                return textfield;
            }

            case "spriteImage" -> {
                SpriteAnimation spriteAnimation = new SpriteAnimation(componentAttributes);
                spriteAnimation.setBounds(componentAttributes.xPos,componentAttributes.yPos,componentAttributes.width,componentAttributes.height);
                spriteAnimation.setName(componentAttributes.imageIcon);
                return  spriteAnimation;
            }

            case "button" -> {
                buttonStyle = new ButtonStyle(style);
                Button button = new Button(LANG.getString(componentAttributes.localeKey));
                buttonStyle.apply(button);
                button.setName(componentAttributes.localeKey);
                button.setActionCommand(componentAttributes.componentId);
                button.setBounds(componentAttributes.xPos, componentAttributes.yPos, buttonStyle.width, buttonStyle.height);
                button.addActionListener(appFrame);
                return button;
            }

            case "multiButton" -> {
                multiButtonStyle = new MultiButtonStyle(style, componentAttributes);
                MultiButton multiButton = new MultiButton();
                multiButtonStyle.apply(multiButton);
                multiButton.setName(componentAttributes.localeKey);
                multiButton.setActionCommand(componentAttributes.componentId);
                multiButton.setBounds(componentAttributes.xPos, componentAttributes.yPos, style.width, style.height);
                return multiButton;
            }

            default -> throw new IllegalArgumentException("Unsupported component type: " + componentType);
        }
    }
}
