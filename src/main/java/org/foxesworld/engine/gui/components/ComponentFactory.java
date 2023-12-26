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
import org.foxesworld.engine.gui.components.scrollBar.CustomScrollBar;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxStyle;
import org.foxesworld.engine.gui.components.serverBox.ServerBox;
import org.foxesworld.engine.gui.components.serverBox.ServerBoxStyle;
import org.foxesworld.engine.gui.components.slider.Slider;
import org.foxesworld.engine.gui.components.slider.TexturedSliderUI;
import org.foxesworld.engine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.engine.gui.components.textfield.Textfield;
import org.foxesworld.engine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class ComponentFactory {

    public Engine engine;
    private final LanguageProvider LANG;
    private final Map<String, Map<String, StyleProvider.StyleAttributes>> componentStyles = new HashMap<>();

    public StyleProvider.StyleAttributes style = null;
    private ComponentAttributes componentAttribute;
    private String[] scrollBoxArr = {""};
    private ComponentFactoryListener componentFactoryListener;

    public ComponentFactory(Engine engine){
        this.engine = engine;
        this.LANG = engine.getLANG();
    }
    public JComponent createComponent(ComponentAttributes componentAttributes) {
        componentFactoryListener.onComponentCreation(componentAttributes);
        if(componentAttributes.getComponentStyle() != null && componentAttributes.getComponentStyle() != null) {
            if(componentStyles.get(componentAttributes.getComponentStyle()) == null){
                componentStyles.put(componentAttributes.getComponentType(), engine.getStyleProvider().getElementStyles().get(componentAttributes.getComponentType()));
            }
            style = componentStyles.get(componentAttributes.getComponentType()).get(componentAttributes.getComponentStyle());
        }
        String[] bounds = componentAttributes.getBounds().split(",");
        int xPos = Integer.parseInt(bounds[0]);
        int yPos = Integer.parseInt(bounds[1]);
        int width = Integer.parseInt(bounds[2]);
        int height = Integer.parseInt(bounds[3]);
        this.componentAttribute = componentAttributes;
        switch (componentAttributes.getComponentType()) {

            case "progressBar" -> {
                ProgressBarStyle progressBarStyle = new ProgressBarStyle(this);
                JProgressBar progressBar = new JProgressBar();
                progressBarStyle.apply(progressBar);
                progressBar.setName(componentAttributes.getComponentId());
                progressBar.setBounds(xPos, yPos, width, height);
                return progressBar;
            }

            case "label" -> {
                LabelStyle labelStyle = new LabelStyle(this);
                Label label = new Label(LANG.getString(componentAttributes.getLocaleKey()));
                labelStyle.apply(label);
                if(componentAttributes.getImageIcon() != null) {
                    ImageIcon icon = new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(componentAttributes.getImageIcon()), componentAttributes.getIconWidth(), componentAttributes.getIconHeight()));
                    if(componentAttribute.isRounded()) {
                        icon = new ImageIcon(this.getRoundedImage(icon.getImage(), componentAttributes.getIconWidth(), componentAttributes.getIconHeight()));
                    }
                    label.setIcon(icon);
                }

                label.setFont(this.engine.getFONTUTILS().getFont(style.getFont(), componentAttributes.getFontSize()));
                labelStyle.apply(label);
                label.setName(componentAttributes.getComponentId());
                label.setBounds(xPos, yPos, width, height);

                if(componentAttributes.getInitialValue() != null) {
                    label.setText(LANG.getString(componentAttributes.getLocaleKey()) + " " + componentAttributes.getInitialValue());
                }

                if(componentAttributes.getColor() != null) {
                    label.setForeground(hexToColor(componentAttributes.getColor()));
                }

                return label;
            }

            case "checkBox" -> {
                CheckboxStyle checkboxStyle = new CheckboxStyle(this);
                Checkbox checkbox = new Checkbox(this, LANG.getString(componentAttributes.getLocaleKey()));
                checkboxStyle.apply(checkbox);
                checkbox.setBounds(xPos, yPos, width, height);
                checkbox.setName(componentAttributes.getComponentId());
                checkbox.setEnabled(componentAttributes.isEnabled());
                if(componentAttributes.getInitialValue() != null) {
                    checkbox.setSelected(Boolean.parseBoolean(componentAttributes.getInitialValue()));
                }
                if (componentAttributes.getKeyCode() != null) {
                    checkbox.setFocusable(true);
                    checkbox.requestFocus();
                    KeyStroke keyStroke = KeyStroke.getKeyStroke(componentAttributes.getKeyCode());
                    AbstractAction buttonAction = new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            checkbox.toggleCheckbox();
                            checkbox.doClick();
                        }
                    };

                    checkbox.getActionMap().put(componentAttributes.getComponentId(), buttonAction);
                    checkbox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, componentAttributes.getComponentId());
                }
                return checkbox;
            }

            case "textField" -> {
                TextfieldStyle textfieldStyle = new TextfieldStyle(this);
                Textfield textfield = new Textfield(LANG.getString(componentAttributes.getLocaleKey()));
                textfieldStyle.apply(textfield);
                textfield.setName(componentAttributes.getComponentId());
                textfield.setBounds(xPos, yPos, textfieldStyle.width, textfieldStyle.height);
                textfield.setActionCommand(componentAttributes.getComponentId());
                textfield.addActionListener(engine);
                if(componentAttributes.getInitialValue() != null) textfield.setText(componentAttributes.getInitialValue());
                return textfield;
            }

            case "passField" -> {
                PassFieldStyle passfieldStyle = new PassFieldStyle(this);
                PassField passfield = new PassField(LANG.getString(componentAttributes.getLocaleKey()));
                passfieldStyle.apply(passfield);
                passfield.setName(componentAttributes.getComponentId());
                passfield.setBounds(xPos, yPos, style.getWidth(), style.getHeight());
                passfield.setFont(this.engine.getFONTUTILS().getFont(style.getFont(), style.getFontSize()));
                passfield.setActionCommand(componentAttributes.getComponentId());
                return passfield;
            }

            case "spriteImage" -> {
                SpriteAnimation spriteAnimation = new SpriteAnimation(componentAttributes);
                spriteAnimation.setOpaque(style.isOpaque());
                spriteAnimation.setBounds(xPos,yPos,width,height);
                spriteAnimation.setName(componentAttributes.getComponentId());
                return  spriteAnimation;
            }

            case "button" -> {
                ButtonStyle buttonStyle = new ButtonStyle(this);
                Button button;
                if (componentAttributes.getImageIcon() != null) {
                    ImageIcon icon = new ImageIcon(ImageUtils.getScaledImage(ImageUtils.getLocalImage(componentAttributes.getImageIcon()), componentAttributes.getIconWidth(), componentAttributes.getIconHeight()));
                    button = new Button(this, icon);
                } else {
                    button = new Button(this, LANG.getString(componentAttributes.getLocaleKey()));
                }

                buttonStyle.apply(button);
                button.setName(componentAttributes.getComponentId());
                button.setActionCommand(componentAttributes.getComponentId());
                button.setBounds(xPos, yPos, width, height);
                button.setEnabled(componentAttributes.isEnabled());
                button.addActionListener(engine);
                if (componentAttributes.getKeyCode() != null) {
                    button.setFocusable(true);
                    button.requestFocus();
                    KeyStroke keyStroke = KeyStroke.getKeyStroke(componentAttributes.getKeyCode());
                    AbstractAction buttonAction = new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            button.ButtonClick();
                            button.doClick();
                            button.setPressed(false);
                        }
                    };

                    button.getActionMap().put(componentAttributes.getComponentId(), buttonAction);
                    button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, componentAttributes.getComponentId());
                }
                return button;
            }

            case "multiButton" -> {
                MultiButtonStyle multiButtonStyle = new MultiButtonStyle(this, componentAttributes);
                MultiButton multiButton = new MultiButton(this);
                multiButtonStyle.apply(multiButton);
                multiButton.setName(componentAttributes.getComponentId());
                multiButton.setActionCommand(componentAttributes.getComponentId());
                multiButton.setBounds(xPos, yPos, style.getWidth(), style.getHeight());
                multiButton.addActionListener(engine);
                return multiButton;
            }

            case "dropBox" -> {
                DropBoxStyle dropBoxStyle = new DropBoxStyle(this);
                DropBox dropBox = new DropBox(this, this.scrollBoxArr, yPos);
                dropBoxStyle.apply(dropBox);
                dropBox.setBounds(xPos,yPos, width,height);
                dropBox.setName(componentAttributes.getComponentId());
                dropBox.setSelectedIndex(componentAttributes.getSelectedIndex());
                dropBox.repaint();
                return dropBox;
            }

            case "serverBox" -> {
                ServerBoxStyle serverBoxStyle = new ServerBoxStyle(this);
                ServerBox serverBox = new ServerBox();
                serverBox.updateBox(componentAttributes.getComponentId(), ImageUtils.getLocalImage("assets/ui/icons/status.png").getSubimage(16, 0, 16, 16));
                serverBoxStyle.apply(serverBox);
                serverBox.setBounds(xPos,yPos, width,height);
                serverBox.setBackground(hexToColor(componentAttributes.getColor()));
                serverBox.setForeground(hexToColor(componentAttributes.getColor()));
                serverBox.setName(componentAttributes.getComponentId());
                return serverBox;
            }

            case "scrollBar" -> {
                CustomScrollBar customScrollBar = new CustomScrollBar(0, 100, 10);
                customScrollBar.setEnabled(componentAttributes.isEnabled());
                customScrollBar.setBounds(xPos, yPos, width, height);
                customScrollBar.setName(componentAttributes.getComponentId());
                return  customScrollBar;
            }

            case "slider" -> {
                Slider slider = new Slider(this);
                slider.setEnabled(componentAttributes.isEnabled());
                slider.setName(componentAttributes.getComponentId());
                slider.setBounds(xPos, yPos, width, height);
                slider.setOpaque(style.isOpaque());
                if(componentAttributes.getInitialValue() != null) {
                    slider.setValue(Integer.parseInt(componentAttributes.getInitialValue()));
                }
                if(style.getThumbImage() != "" & style.getTrackImage() != "") {
                    slider.setUI(new TexturedSliderUI(slider, style.getThumbImage(), style.getTrackImage()));
                }

                return slider;
            }

            default -> throw new IllegalArgumentException("Unsupported component type: " + componentAttributes.getComponentType());
        }
    }

    public void setComponentFactoryListener(ComponentFactoryListener componentFactoryListener) {
        this.componentFactoryListener = componentFactoryListener;
    }

    private Image getRoundedImage(Image image, int width, int height) {
        BufferedImage roundedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = roundedImage.createGraphics();
        g2.setClip(new Ellipse2D.Float(0, 0, width, height));
        g2.drawImage(image, 0, 0, width, height, null);
        g2.dispose();
        return roundedImage;
    }

    public enum Align {
        LEFT, CENTER, RIGHT
    }

    public void setScrollBoxArr(String[] scrollBoxArr) {
        this.scrollBoxArr = scrollBoxArr;
    }

    public ComponentAttributes getComponentAttribute() {
        return componentAttribute;
    }
}
