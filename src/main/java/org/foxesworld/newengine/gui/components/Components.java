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
import org.foxesworld.newengine.gui.components.passfield.PassField;
import org.foxesworld.newengine.gui.components.passfield.PassFieldStyle;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyle;
import org.foxesworld.newengine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.newengine.gui.components.textfield.Textfield;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.locale.LanguageProvider;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;

public class Components {

    public AppFrame appFrame;
    private LanguageProvider LANG;
    private TextfieldStyle textfieldStyle;

    private PassFieldStyle passfieldStyle;
    private ProgressBarStyle progressBarStyle;
    private LabelStyle labelStyle;
    private ButtonStyle buttonStyle;
    private CheckboxStyle checkboxStyle;
    private MultiButtonStyle multiButtonStyle;
    public StyleProvider.StyleAttributes style = null;

    public Components(AppFrame appFrame){
        this.appFrame = appFrame;
        this.LANG = appFrame.getLANG();
    }

    public JComponent createComponent(ComponentAttributes componentAttributes) {

        if(appFrame.getElementStyles().get(componentAttributes.componentType)!=null) {
            style = appFrame.getElementStyles().get(componentAttributes.componentType).get(componentAttributes.componentStyle);
        }
        String[] bounds = componentAttributes.bounds.split(",");
        int xPos = Integer.parseInt(bounds[0]);
        int yPos = Integer.parseInt(bounds[1]);
        int width = Integer.parseInt(bounds[2]);
        int height = Integer.parseInt(bounds[3]);
        switch (componentAttributes.componentType) {

            case "progressBar" -> {
                progressBarStyle = new ProgressBarStyle(this);
                JProgressBar progressBar = new JProgressBar();
                progressBarStyle.apply(progressBar);
                progressBar.setName(componentAttributes.componentId);
                progressBar.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                return progressBar;
            }

            case "label" -> {
                labelStyle = new LabelStyle(this);
                Label label = new Label(LANG.getString(componentAttributes.localeKey));
                labelStyle.apply(label);
                if(componentAttributes.imageIcon != null) {
                    label.setIcon(new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(componentAttributes.imageIcon), componentAttributes.iconWidth, componentAttributes.iconHeight)));
                }
                label.setFont(this.appFrame.getFontUtils().getFont(style.font, componentAttributes.fontSize));
                labelStyle.apply(label);
                label.setName(componentAttributes.componentId);
                label.setBounds(xPos, yPos, width, height);
                return label;
            }

            case "checkBox" -> {
                checkboxStyle = new CheckboxStyle(this);
                Checkbox checkbox = new Checkbox(LANG.getString(componentAttributes.localeKey));
                checkboxStyle.apply(checkbox);
                checkbox.setBounds(xPos, yPos, width, height);
                checkbox.setName(componentAttributes.componentId);
                return checkbox;
            }

            case "textField" -> {
                textfieldStyle = new TextfieldStyle(this);
                Textfield textfield = new Textfield(LANG.getString(componentAttributes.localeKey));
                textfieldStyle.apply(textfield);
                textfield.setName(componentAttributes.componentId);
                textfield.setBounds(xPos, yPos, textfieldStyle.width, textfieldStyle.height);
                textfield.setActionCommand(componentAttributes.componentId);
                textfield.addActionListener(appFrame);
                return textfield;
            }

            case "passField" -> {
                passfieldStyle = new PassFieldStyle(this);
                PassField passfield = new PassField(LANG.getString(componentAttributes.localeKey));
                passfieldStyle.apply(passfield);
                passfield.setName(componentAttributes.componentId);
                passfield.setBounds(xPos, yPos, style.width, style.height);
                passfield.setFont(this.appFrame.getFontUtils().getFont(style.font, style.fontSize));
                passfield.setActionCommand(componentAttributes.componentId);
                return passfield;
            }

            case "spriteImage" -> {
                SpriteAnimation spriteAnimation = new SpriteAnimation(componentAttributes);
                spriteAnimation.setBounds(xPos,yPos,width,height);
                spriteAnimation.setName(componentAttributes.componentId);
                return  spriteAnimation;
            }

            case "button" -> {
                buttonStyle = new ButtonStyle(this);
                Button button = new Button(LANG.getString(componentAttributes.localeKey));
                buttonStyle.apply(button);
                button.setName(componentAttributes.localeKey);
                button.setActionCommand(componentAttributes.componentId);
                button.setBounds(xPos, yPos, buttonStyle.width, buttonStyle.height);
                button.addActionListener(appFrame);
                return button;
            }

            case "multiButton" -> {
                multiButtonStyle = new MultiButtonStyle(style, componentAttributes);
                MultiButton multiButton = new MultiButton();
                multiButtonStyle.apply(multiButton);
                multiButton.setName(componentAttributes.componentId);
                multiButton.setActionCommand(componentAttributes.componentId);
                multiButton.setBounds(xPos, yPos, style.width, style.height);
                multiButton.addActionListener(appFrame);
                return multiButton;
            }

            default -> throw new IllegalArgumentException("Unsupported component type: " + componentAttributes.componentType);
        }
    }
}
