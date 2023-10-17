package org.foxesworld.newengine.gui.components.frame;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.gui.components.panel.Panel;
import org.foxesworld.newengine.locale.LanguageProvider;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;

public class Frame {
    private AppFrame appFrame;
    private Panel panel;
    private Dimension screenSize;
    private JPanel rootPanel;
    private final JFrame frame;
    private final LanguageProvider LANG;

    public Frame(AppFrame appFrame) {
        APP.LOGGER.info("Frame initialization");
        this.appFrame = appFrame;
        this.frame = new JFrame();
        this.LANG = appFrame.getApp().getLANG();
    }

    public void buildFrame(FrameAttributes frameAttributes) {
        APP.LOGGER.info("Building Frame...");

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
