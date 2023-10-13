package org.foxesworld.newengine.gui;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.DownloadUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AppFrame extends JFrame implements ActionListener {
    protected final APP app;
    private GuiBuilder guiBuilder;
    private Map<String, Map<String, StyleProvider.StyleAttributes>> elementStyles = new HashMap<>();
    private JFrame frame;
    private DownloadUtils download;
    protected final LanguageProvier LANG;

    public AppFrame(APP app) {
        this.LANG = app.getLANG();
        this.app = app;
        this.frame = new JFrame();
        initialize();
    }

    private void initialize() {
        StyleProvider styleProvider = new StyleProvider();
        this.elementStyles = styleProvider.getElementStyles();
        guiBuilder = new GuiBuilder(this);
        this.loadFrames();

        //=================================
        displayId("test", true);
        displayId("download", false);

        this.download = new DownloadUtils(this.guiBuilder);
        //this.download.download("https://cdimage.debian.org/cdimage/archive/11.7.0/amd64/iso-cd/debian-11.7.0-amd64-netinst.iso", "");
    }

    public void displayId(String id, boolean visible){
        for(Map.Entry<String, List<Component>> entryMap: guiBuilder.getComponentsMap().entrySet()){
            for (Component component : entryMap.getValue()) {
                frame.add(component);
                if(entryMap.getKey().equals(id)) {
                    component.setVisible(visible);
                }

                if(entryMap.getKey().equals("download")) {
                    if (component instanceof JProgressBar) {
                        this.guiBuilder.setProgressBar((JProgressBar) component);
                    }

                    if(component instanceof JLabel){
                        this.guiBuilder.setProgressLabel((JLabel) component);
                    }
                }
            }
        }
    }

    private void loadFrames(){
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Frame.class.getClassLoader().getResourceAsStream("loadFrames.json")), StandardCharsets.UTF_8);
        FrameListAttributes[] array = gson.fromJson(reader, FrameListAttributes[].class);
        for (FrameListAttributes obj : array) {
            String framePath = obj.framePath;
            boolean inputStream = obj.inputStream;
            this.guiBuilder.buildGui(framePath, inputStream);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getSource());
    }

    private class FrameListAttributes {
        @SerializedName("framePath")
        String framePath;

        @SerializedName("inputStream")
        boolean inputStream;
    }

    public Map<String, Map<String, StyleProvider.StyleAttributes>> getElementStyles() {
        return elementStyles;
    }

    public JFrame getFrame() {
        return this.frame;
    }

    public APP getApp() {
        return this.app;
    }
}
