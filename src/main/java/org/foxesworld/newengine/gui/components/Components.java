package org.foxesworld.newengine.gui.components;

import org.foxesworld.newengine.gui.AppFrame;
import org.foxesworld.newengine.gui.attributes.ComponentAttributes;
import org.foxesworld.newengine.gui.components.button.ButtonStyle;
import org.foxesworld.newengine.gui.components.button.ButtonStyleFactory;
import org.foxesworld.newengine.gui.components.button.StyledButton;
import org.foxesworld.newengine.gui.components.label.LabelStyle;
import org.foxesworld.newengine.gui.components.label.LabelStyleFactory;
import org.foxesworld.newengine.gui.components.label.StyledLabel;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyle;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyleFactory;
import org.foxesworld.newengine.gui.components.progressBar.StyledProgressBar;
import org.foxesworld.newengine.gui.components.textfield.StyledTextfield;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyleFactory;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;

public class Components {

    private AppFrame appFrame;

    private LanguageProvier LANG;
    private TextfieldStyleFactory textfieldStyleFactory;
    private ProgressBarStyleFactory progressBarStyleFactory;
    private LabelStyleFactory labelStyleFactory;
    private ButtonStyleFactory buttonStyleFactory;

    public Components(AppFrame appFrame){
        this.appFrame = appFrame;
        this.LANG = appFrame.getApp().getLANG();
    }

    public JComponent createComponent(ComponentAttributes componentAttributes, String componentType) {
        switch (componentType) {

            case "progressBar" -> {
                progressBarStyleFactory = new ProgressBarStyleFactory(appFrame.getElementStyles().get("progressBar").get(componentAttributes.componentStyle));
                ProgressBarStyle progressBarStyle = this.progressBarStyleFactory.getProgressBarStyle();
                StyledProgressBar progressBar = new StyledProgressBar(progressBarStyle);
                progressBar.setBounds(componentAttributes.xPos, componentAttributes.yPos, componentAttributes.width, componentAttributes.height);
                return progressBar;
            }

            case "label" -> {
                labelStyleFactory = new LabelStyleFactory(appFrame.getElementStyles().get("label").get(componentAttributes.componentStyle));
                LabelStyle labelStyle = this.labelStyleFactory.getLabelStyle();
                StyledLabel label = new StyledLabel(LANG.getString(componentAttributes.localeKey), labelStyle);
                labelStyle.apply(label);
                label.setBounds(componentAttributes.xPos, componentAttributes.yPos, 90, 50);
                return label;
            }
            case "textField" -> {
                textfieldStyleFactory = new TextfieldStyleFactory(appFrame.getElementStyles().get("input").get(componentAttributes.componentStyle));
                TextfieldStyle textfieldStyle = this.textfieldStyleFactory.getTextfieldStyle();
                StyledTextfield textfield = new StyledTextfield(textfieldStyle);
                textfieldStyle.apply(textfield);
                textfield.setBounds(componentAttributes.xPos, componentAttributes.yPos, textfieldStyle.width, textfieldStyle.height);
                return textfield;
            }
            case "button" -> {
                buttonStyleFactory = new ButtonStyleFactory(appFrame.getElementStyles().get("button").get(componentAttributes.componentStyle));
                ButtonStyle buttonStyle = this.buttonStyleFactory.getButtonStyle();
                StyledButton button = new StyledButton(LANG.getString(componentAttributes.localeKey), buttonStyle);
                buttonStyle.apply(button);
                button.setBounds(componentAttributes.xPos, componentAttributes.yPos, buttonStyle.width, buttonStyle.height);
                button.addActionListener(e -> {
                });
                return button;
            }
            default -> throw new IllegalArgumentException("Unsupported component type: " + componentType);
        }
    }
}
