package org.foxesworld.newengine.gui.components.frame;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;

public class Frame {
    private Dimension screenSize;
    private final JFrame frame;
    private JPanel panel;
    private final LanguageProvier LANG;

    public Frame(AppFrame appFrame) {
        APP app = appFrame.getApp();
        this.frame = appFrame.getFrame();
        this.LANG = app.getLANG();
    }


    public void buildFrame(FrameAttributes frameAttributes){
        frame.setTitle(LANG.getString(frameAttributes.title));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameAttributes.width, frameAttributes.height);
        frame.setResizable(frameAttributes.resizable);
        frame.setUndecorated(frameAttributes.undecorated);

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        frame.setLayout(null);

        //WIP
        this.panel = new JPanel() {
            @Override
            public void paintComponent(Graphics gmain) {
                Graphics2D g = (Graphics2D) gmain;
                g.drawImage(ImageUtils.getLocalImage("/assets/bg_summer.png"), 0, 0, screenSize.width, screenSize.height,null);
            }
        };
        this.frame.add(this.panel);
        //================
    }

}