package org.foxesworld.newengine.gui;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.button.ButtonStyle;
import org.foxesworld.newengine.gui.components.button.ButtonStyleFactory;
import org.foxesworld.newengine.gui.components.button.StyledButton;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.foxesworld.newengine.gui.components.Align.CENTER;

public class GuiBuilder {
    private HashMap<String, List<Component>> componentsMap = new HashMap<>();
    private ButtonStyleFactory buttonStyleFactory;
    private APP app;
    private  JFrame frame;
    private LanguageProvier LANG;

    public GuiBuilder(Frame frame){
        this.app = frame.getApp();
        this.frame = frame.getFrame();
        buttonStyleFactory = new ButtonStyleFactory(app, frame.getElementStyles().get("button"));
        this.LANG = app.getLANG();
    }
    public void buildGui(String path) {
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(GuiBuilder.class.getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8);
        FrameProperties frameProperties = gson.fromJson(reader, FrameProperties.class);

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

        for (FrameComponents frameComponents : frameProperties.fields) {
            JComponent component = createComponent(frameComponents);
            this.addComponentToMap(frameComponents.frameGroup, component);
        }
        frame.setVisible(true);
    }

    private JComponent createComponent(FrameComponents frameComponents) {
        if ("label".equals(frameComponents.type)) {
            JLabel label = new JLabel(LANG.getString(frameComponents.text));
            label.setBounds(frameComponents.x, frameComponents.y, frameComponents.width, frameComponents.height);
            return label;
        } else if ("textfield".equals(frameComponents.type)) {
            JTextField textField = new JTextField();
            textField.setBounds(frameComponents.x, frameComponents.y, frameComponents.width, frameComponents.height);
            return textField;
        } else if ("button".equals(frameComponents.type)) {
            ButtonStyle buttonStyle = this.buttonStyleFactory.getButtonStyle(frameComponents.style);
            StyledButton button = new StyledButton(LANG.getString(frameComponents.text), buttonStyle);
            buttonStyle.apply(button);
            button.setBounds(frameComponents.x, frameComponents.y, frameComponents.width, frameComponents.height);
            button.addActionListener(e -> {});
            return button;
        } else {
            throw new IllegalArgumentException("Unsupported component type: " + frameComponents.type);
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
    List<FrameComponents> fields;
}

class FrameComponents {
    @SerializedName("type")
    String type;

    @SerializedName("frameGroup")
    String frameGroup;

    @SerializedName("style")
    String style;

    @SerializedName("text")
    String text;

    @SerializedName("x")
    int x;

    @SerializedName("y")
    int y;

    @SerializedName("width")
    int width;

    @SerializedName("height")
    int height;
}

