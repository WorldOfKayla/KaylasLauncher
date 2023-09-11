package org.foxesworld.newengine.gui;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.button.ButtonStyle;
import org.foxesworld.newengine.gui.components.button.ButtonStyleFactory;
import org.foxesworld.newengine.gui.components.button.StyledButton;
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

import static org.foxesworld.newengine.utils.FontUtils.getFont;
import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

public class FrameBuilder {
    private final HashMap<String, List<Component>> componentsMap = new HashMap<>();
    private ButtonStyleFactory buttonStyleFactory;
    private AppFrame appFrame;
    private TextfieldStyleFactory textfieldStyleFactory;
    private final JFrame frame;
    private final LanguageProvier LANG;

    public FrameBuilder(AppFrame appFrame) {
        APP app = appFrame.getApp();
        this.frame = appFrame.getFrame();
        this.appFrame = appFrame;
        //buttonStyleFactory = new ButtonStyleFactory(appFrame.getElementStyles().get("button"));
        //textfieldStyleFactory = new TextfieldStyleFactory(appFrame.getElementStyles().get("input"));
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

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        frame.setLayout(null);

        for (Map.Entry<String, List<FrameComponent>> entry : frameProperties.fields.entrySet()) {
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
            case "labels" -> {
                JLabel label = new JLabel(LANG.getString(frameComponent.text));
                label.setBounds(frameComponent.x, frameComponent.y, frameComponent.width, frameComponent.height);
                label.setFont(getFont(frameComponent.fontName, frameComponent.fontSize));
                label.setForeground(hexToColor(frameComponent.color));
                return label;
            }
            case "textfields" -> {
                textfieldStyleFactory = new TextfieldStyleFactory(appFrame.getElementStyles().get("input").get(frameComponent.style));
                System.out.println(appFrame.getElementStyles().get("input").get(frameComponent.style).name);
                TextfieldStyle textfieldStyle = this.textfieldStyleFactory.getTextfieldStyle();
                StyledTextfield textfield = new StyledTextfield(textfieldStyle);
                textfieldStyle.apply(textfield);
                //textfield.setFont(getFont(frameComponent.fontName, frameComponent.fontSize));
                textfield.setBounds(frameComponent.x, frameComponent.y, frameComponent.width, frameComponent.height);
                //textfield.setForeground(hexToColor(frameComponent.color));
                //textfield.setBorder(border);
                return textfield;
            }
            case "buttons" -> {
                buttonStyleFactory = new ButtonStyleFactory(appFrame.getElementStyles().get("button").get(frameComponent.style));
                ButtonStyle buttonStyle = this.buttonStyleFactory.getButtonStyle();
                StyledButton button = new StyledButton(LANG.getString(frameComponent.text), buttonStyle);
                buttonStyle.apply(button);
                button.setBounds(frameComponent.x, frameComponent.y, frameComponent.width, frameComponent.height);
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

    @SerializedName("fields")
    Map<String, List<FrameComponent>> fields;
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
