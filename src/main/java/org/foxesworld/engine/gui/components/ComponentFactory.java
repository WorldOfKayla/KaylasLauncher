package org.foxesworld.engine.gui.components;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.button.ButtonStyle;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.checkbox.CheckboxStyle;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.label.LabelStyle;
import org.foxesworld.engine.gui.components.multiButton.MultiButton;
import org.foxesworld.engine.gui.components.multiButton.MultiButtonStyle;
import org.foxesworld.engine.gui.components.passfield.PassField;
import org.foxesworld.engine.gui.components.passfield.PassFieldStyle;
import org.foxesworld.engine.gui.components.progressBar.ProgressBarStyle;
import org.foxesworld.engine.gui.components.scrollBox.ScrollBox;
import org.foxesworld.engine.gui.components.scrollBox.ScrollBoxStyle;
import org.foxesworld.engine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.engine.gui.components.textfield.Textfield;
import org.foxesworld.engine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ComponentFactory {

    public Engine engine;
    private LanguageProvider LANG;
    private Map<String, Map<String, StyleProvider.StyleAttributes>> componentStyles = new HashMap<>();
    private TextfieldStyle textfieldStyle;
    private PassFieldStyle passfieldStyle;
    private ProgressBarStyle progressBarStyle;
    private LabelStyle labelStyle;
    private ButtonStyle buttonStyle;
    private CheckboxStyle checkboxStyle;
    private MultiButtonStyle multiButtonStyle;
    private ScrollBoxStyle scrollBoxStyle;
    public StyleProvider.StyleAttributes style = null;

    public ComponentFactory(Engine engine){
        this.engine = engine;
        this.LANG = engine.getLANG();
    }
    public JComponent createComponent(ComponentAttributes componentAttributes) {
        /*
        * TODO
        *  We should try supplying an InitialValue
        *  That's the only way we may use Component factory for initialising a couple of
        *  components of a type
        *  */
        Object initialData = "";
        if(componentAttributes.componentType != null && componentAttributes.componentStyle != null) {
            if(componentStyles.get(componentAttributes.componentType) == null){
                componentStyles.put(componentAttributes.componentType, engine.getStyleProvider().loadStyle(componentAttributes.componentType));
            }
            style = componentStyles.get(componentAttributes.componentType).get(componentAttributes.componentStyle);
        }
        String[] bounds = componentAttributes.bounds.split(",");
        int xPos = Integer.parseInt(bounds[0]);
        int yPos = Integer.parseInt(bounds[1]);
        int width = Integer.parseInt(bounds[2]);
        int height = Integer.parseInt(bounds[3]);
        if(componentAttributes.initialValue != null) {
            initialData = this.getInitialData(componentAttributes.initialValue);
        }
        switch (componentAttributes.componentType) {

            case "progressBar" -> {
                progressBarStyle = new ProgressBarStyle(this);
                JProgressBar progressBar = new JProgressBar();
                progressBarStyle.apply(progressBar);
                progressBar.setName(componentAttributes.componentId);
                progressBar.setBounds(xPos, yPos, width, height);
                return progressBar;
            }

            case "label" -> {
                labelStyle = new LabelStyle(this);
                Label label = new Label(LANG.getString(componentAttributes.localeKey));
                labelStyle.apply(label);
                if(componentAttributes.imageIcon != null) {
                    label.setIcon(new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(componentAttributes.imageIcon), componentAttributes.iconWidth, componentAttributes.iconHeight)));
                }
                label.setFont(this.engine.getFONTUTILS().getFont(style.font, componentAttributes.fontSize));
                labelStyle.apply(label);
                label.setName(componentAttributes.componentId);
                label.setBounds(xPos, yPos, width, height);

                if(componentAttributes.initialValue != null) {
                    label.setText(String.valueOf(initialData));
                }
                return label;
            }

            case "checkBox" -> {
                checkboxStyle = new CheckboxStyle(this);
                Checkbox checkbox = new Checkbox(this, LANG.getString(componentAttributes.localeKey));
                checkboxStyle.apply(checkbox);
                checkbox.setBounds(xPos, yPos, width, height);
                checkbox.setName(componentAttributes.componentId);
                checkbox.setEnabled(componentAttributes.enabled);
                if(componentAttributes.initialValue != null) checkbox.setSelected((Boolean) initialData);
                return checkbox;
            }

            case "textField" -> {
                textfieldStyle = new TextfieldStyle(this);
                Textfield textfield = new Textfield(LANG.getString(componentAttributes.localeKey));
                textfieldStyle.apply(textfield);
                textfield.setName(componentAttributes.componentId);
                textfield.setBounds(xPos, yPos, textfieldStyle.width, textfieldStyle.height);
                textfield.setActionCommand(componentAttributes.componentId);
                textfield.addActionListener(engine);
                if(componentAttributes.initialValue != null) textfield.setText(String.valueOf(initialData));
                return textfield;
            }

            case "passField" -> {
                passfieldStyle = new PassFieldStyle(this);
                PassField passfield = new PassField(LANG.getString(componentAttributes.localeKey));
                passfieldStyle.apply(passfield);
                passfield.setName(componentAttributes.componentId);
                passfield.setBounds(xPos, yPos, style.width, style.height);
                passfield.setFont(this.engine.getFONTUTILS().getFont(style.font, style.fontSize));
                passfield.setActionCommand(componentAttributes.componentId);
                return passfield;
            }

            case "spriteImage" -> {
                SpriteAnimation spriteAnimation = new SpriteAnimation(componentAttributes);
                spriteAnimation.setOpaque(false);
                spriteAnimation.setBounds(xPos,yPos,width,height);
                spriteAnimation.setName(componentAttributes.componentId);
                return  spriteAnimation;
            }

            case "button" -> {
                buttonStyle = new ButtonStyle(this);
                Button button = new Button(this, LANG.getString(componentAttributes.localeKey));
                if(componentAttributes.imageIcon != null){
                    ImageIcon icon = new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(componentAttributes.imageIcon), componentAttributes.iconWidth, componentAttributes.iconHeight));
                    button = new Button(this, icon);
                }
                buttonStyle.apply(button);
                button.setName(componentAttributes.componentId);
                button.setActionCommand(componentAttributes.componentId);
                button.setBounds(xPos, yPos, width, height);
                button.addActionListener(engine);
                return button;
            }

            case "multiButton" -> {
                multiButtonStyle = new MultiButtonStyle(this, componentAttributes);
                MultiButton multiButton = new MultiButton(this);
                multiButtonStyle.apply(multiButton);
                multiButton.setName(componentAttributes.componentId);
                multiButton.setActionCommand(componentAttributes.componentId);
                multiButton.setBounds(xPos, yPos, style.width, style.height);
                multiButton.addActionListener(engine);
                return multiButton;
            }

            case "scrollBox" -> {
               /*
                * TODO
                *  We should never initialize a* single component in ComponentFactory, never...
                *  Never...
                *  A temporary solution...
                */
                String scrollBoxArr[] = {};
                scrollBoxStyle = new ScrollBoxStyle(this);
                switch (componentAttributes.initialValue) {
                    case "userServers" -> scrollBoxArr  = engine.getAuth().getUserServersArray();
                }
                ScrollBox scrollBox = new ScrollBox(this, scrollBoxArr, yPos);
                scrollBoxStyle.apply(scrollBox);
                scrollBox.setBounds(xPos,yPos, width,height);
                scrollBox.setName(componentAttributes.componentId);
                return  scrollBox;
            }

            default -> throw new IllegalArgumentException("Unsupported component type: " + componentAttributes.componentType);
        }
    }

    private Object getInitialData(String initialValue){
        String[] splitValue = initialValue.split("#");
        switch(splitValue[0]){
            case "config" -> {
                return engine.getCONFIG().getCONFIG().get(splitValue[1]);
            }
            case "user" -> {
                return engine.getAuth().getAuthCredentials(splitValue[1]);
            }
        }
        return null;
    }

    public enum Align {
        LEFT, CENTER, RIGHT
    }
}
