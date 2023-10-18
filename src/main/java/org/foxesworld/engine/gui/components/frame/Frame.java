package org.foxesworld.engine.gui.components.frame;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.AppFrame;
import org.foxesworld.engine.gui.attributes.FrameAttributes;
import org.foxesworld.engine.gui.components.panel.Panel;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Frame {
    private AppFrame appFrame;
    private Panel panel;
    private Dimension screenSize;
    private JPanel rootPanel;
    private final JFrame frame;
    private final LanguageProvider LANG;

    public Frame(AppFrame appFrame) {
        appFrame.getLOGGER().info("Frame initialization");
        this.appFrame = appFrame;
        this.frame = new JFrame();
        this.LANG = appFrame.getLANG();
        buildFrame("assets/frames/frame.json");
    }

    private void buildFrame(String path){
        Gson gson = new Gson();
        FrameAttributes frameAttributes;
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(AppFrame.class.getClassLoader().getResourceAsStream(path)), StandardCharsets.UTF_8);
        frameAttributes = gson.fromJson(reader, FrameAttributes.class);
        buildFrame(frameAttributes);
    }

    public void buildFrame(FrameAttributes frameAttributes) {
        appFrame.getLOGGER().info("Building Frame...");

        frame.setIconImage(ImageUtils.getLocalImage(frameAttributes.appIcon));
        frame.setTitle(LANG.getString(frameAttributes.appTitle));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameAttributes.width, frameAttributes.height);
        frame.setResizable(frameAttributes.resizable);
        frame.setUndecorated(frameAttributes.undecorated);
        frame.setContentPane(new JLabel(new ImageIcon(ImageUtils.getLocalImage(frameAttributes.backgroundImage))));

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        panel = new Panel(this);
        this.rootPanel = panel.setRootPanel(frameAttributes);
        this.rootPanel.setName("rootPanel");
        frame.setContentPane(this.rootPanel);
        frame.setVisible(true);
    }

    public Dimension getScreenSize() {
        return screenSize;
    }

    public JPanel getRootPanel() {
        return this.rootPanel;
    }

    public AppFrame getAppFrame() {
        return appFrame;
    }

    public JFrame getFrame() {
        return frame;
    }

    public Panel getPanel() {
        return panel;
    }
}
