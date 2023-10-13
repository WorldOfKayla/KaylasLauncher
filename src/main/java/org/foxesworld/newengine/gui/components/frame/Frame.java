package org.foxesworld.newengine.gui.components.frame;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.attributes.FrameAttributes;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.foxesworld.newengine.utils.FontUtils.hexToColor;

//WIP
public class Frame {
    private Dimension screenSize;
    private JPanel contentPanel;
    private final JFrame frame;
    private final LanguageProvier LANG;

    public Frame(AppFrame appFrame) {
        APP.LOGGER.info("Frame initialization");
        APP app = appFrame.getApp();
        this.frame = new JFrame();
        this.LANG = app.getLANG();
    }

    public void buildFrame(FrameAttributes frameAttributes) {
        APP.LOGGER.info("Building Frame...");
        frame.setTitle(LANG.getString(frameAttributes.title));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameAttributes.width, frameAttributes.height);
        frame.setResizable(frameAttributes.resizable);
        frame.setUndecorated(frameAttributes.undecorated);
        frame.setContentPane(new JLabel(new ImageIcon(ImageUtils.getLocalImage(frameAttributes.backgroundImage))));

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);

        this.contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawDarkenedBackground(g, frameAttributes);
            }
        };
        this.contentPanel.setOpaque(false);
        this.contentPanel.setLayout(null);
        frame.setContentPane(this.contentPanel);
        frame.setVisible(true);
    }

    private void drawDarkenedBackground(Graphics g, FrameAttributes frameAttributes) {
        BufferedImage background = ImageUtils.getLocalImage(frameAttributes.backgroundImage);
        g.drawImage(background, 0, 0, null);

        g.setColor(hexToColor(frameAttributes.backgroundBlur));
        g.fillRect(0, 0, this.screenSize.width, this.screenSize.height);
    }

    public Dimension getScreenSize() {
        return screenSize;
    }

    public JPanel getContentPanel() {
        return this.contentPanel;
    }

    public JFrame getFrame() {
        return frame;
    }
}
