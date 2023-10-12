package org.foxesworld.newengine.gui;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.button.ButtonStyle;
import org.foxesworld.newengine.gui.components.button.ButtonStyleFactory;
import org.foxesworld.newengine.gui.components.button.StyledButton;
import org.foxesworld.newengine.gui.components.label.LabelStyle;
import org.foxesworld.newengine.gui.components.label.LabelStyleFactory;
import org.foxesworld.newengine.gui.components.label.StyledLabel;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBar;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyle;
import org.foxesworld.newengine.gui.components.progressBar.ProgressBarStyleFactory;
import org.foxesworld.newengine.gui.components.progressBar.StyledProgressBar;
import org.foxesworld.newengine.gui.components.textfield.StyledTextfield;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyle;
import org.foxesworld.newengine.gui.components.textfield.TextfieldStyleFactory;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class FrameBuilder {
    private final HashMap<String, List<Component>> componentsMap = new HashMap<>();
    private ButtonStyleFactory buttonStyleFactory;
    private AppFrame appFrame;
    private  Dimension screenSize;
    private TextfieldStyleFactory textfieldStyleFactory;
    private ProgressBarStyleFactory progressBarStyleFactory;
    private LabelStyleFactory labelStyleFactory;
    private JProgressBar progressBar;

    private final JFrame frame;
    private final LanguageProvier LANG;

    public FrameBuilder(AppFrame appFrame) {
        APP app = appFrame.getApp();
        this.frame = appFrame.getFrame();
        this.appFrame = appFrame;
        this.LANG = app.getLANG();
    }

    public void buildGui(String path) {
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(FrameBuilder.class.getClassLoader().getResourceAsStream(path)), StandardCharsets.UTF_8);
        FrameProperties frameProperties = gson.fromJson(reader, FrameProperties.class);

        frame.setTitle(LANG.getString(frameProperties.title));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameProperties.width, frameProperties.height);
        frame.setResizable(frameProperties.resizable);
        frame.setUndecorated(frameProperties.undecorated);

        frame.setTitle(LANG.getString(frameProperties.title));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameProperties.width, frameProperties.height);
        frame.setResizable(frameProperties.resizable);
        frame.setUndecorated(frameProperties.undecorated);
        frame.setContentPane(new JLabel(new ImageIcon("/assets/bg_summer.png")));

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        frame.setLayout(null);

        for (Map.Entry<String, List<FrameComponent>> entry : frameProperties.inner.entrySet()) {
            String componentType = entry.getKey();
            List<FrameComponent> components = entry.getValue();
            for (FrameComponent frameComponent : components) {
                JComponent component = createComponent(frameComponent, componentType);
                this.addComponentToMap(frameComponent.frameGroup, component);
            }
        }
        frame.setVisible(true);
    }

    private JComponent createComponent(FrameComponent frameComponent, String componentType) {
        switch (componentType) {
            case "progressBars" -> {
                progressBarStyleFactory = new ProgressBarStyleFactory(appFrame.getElementStyles().get("progressBar").get(frameComponent.style));
                ProgressBarStyle progressBarStyle = this.progressBarStyleFactory.getProgressBarStyle();
                StyledProgressBar progressBar = new StyledProgressBar(progressBarStyle);
                progressBar.setBounds(frameComponent.x, frameComponent.y, frameComponent.width, frameComponent.height);
                return progressBar;
            }

            case "labels" -> {
                labelStyleFactory = new LabelStyleFactory(appFrame.getElementStyles().get("label").get(frameComponent.style));
                LabelStyle labelStyle = this.labelStyleFactory.getLabelStyle();
                StyledLabel label = new StyledLabel(LANG.getString(frameComponent.text), labelStyle);
                labelStyle.apply(label);
                label.setBounds(frameComponent.x, frameComponent.y, 90, 50);
                return label;
            }
            case "textfields" -> {
                textfieldStyleFactory = new TextfieldStyleFactory(appFrame.getElementStyles().get("input").get(frameComponent.style));
                TextfieldStyle textfieldStyle = this.textfieldStyleFactory.getTextfieldStyle();
                StyledTextfield textfield = new StyledTextfield(textfieldStyle);
                textfieldStyle.apply(textfield);
                textfield.setBounds(frameComponent.x, frameComponent.y, textfieldStyle.width, textfieldStyle.height);
                //textfield.setBorder(null);
                return textfield;
            }
            case "buttons" -> {
                buttonStyleFactory = new ButtonStyleFactory(appFrame.getElementStyles().get("button").get(frameComponent.style));
                ButtonStyle buttonStyle = this.buttonStyleFactory.getButtonStyle();
                StyledButton button = new StyledButton(LANG.getString(frameComponent.text), buttonStyle);
                buttonStyle.apply(button);
                button.setBounds(frameComponent.x, frameComponent.y, buttonStyle.width, buttonStyle.height);
                button.addActionListener(e -> {
                });
                return button;
            }
            default -> throw new IllegalArgumentException("Unsupported component type: " + componentType);
        }
    }


    private void addComponentToMap(String groupId, Component component) {
        if (!componentsMap.containsKey(groupId)) {
            componentsMap.put(groupId, new ArrayList<>());
        }
        componentsMap.get(groupId).add(component);
    }

    public HashMap<String, List<Component>> getComponentsMap() {
        return componentsMap;
    }

    public void setProgressBar(JProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }
    public Dimension getScreenSize() {
        return screenSize;
    }
}

class FrameProperties {
    @SerializedName("title")
    String title;

    @SerializedName("width")
    int width;

    @SerializedName("height")
    int height;

    @SerializedName("resizable")
    boolean resizable;

    @SerializedName("undecorated")
    boolean undecorated;

    @SerializedName("inner")
    Map<String, List<FrameComponent>> inner;
}

class FrameComponent {
    @SerializedName("frameGroup")
    String frameGroup;

    @SerializedName("style")
    String style;

    @SerializedName("text")
    String text;

    @SerializedName("fontName")
    String fontName;

    @SerializedName("fontSize")
    int fontSize;

    @SerializedName("color")
    String color;

    @SerializedName("x")
    int x;

    @SerializedName("y")
    int y;

    @SerializedName("width")
    int width;

    @SerializedName("height")
    int height;
}
