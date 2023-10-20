package org.foxesworld.engine.gui;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

@Deprecated
public class LoadState extends JPanel {
    private JPanel rootPanel;
    private Timer timer;
    private BufferedImage tmpImage;
    private Engine engine;
    private  int tindex;

    public LoadState(Engine engine) {
        this.engine = engine;
        this.rootPanel = engine.getFrame().getRootPanel();
    }

    public void showLoadingState(int displayTimeMillis) {
        JPanel loadingPanel = this.engine.getGuiBuilder().getPanelsMap().get("wait");
        this.tmpImage = ImageUtils.screenComponent(engine.getFrame().getRootPanel());
        loadingPanel.setBounds(0, 0, rootPanel.getWidth(), rootPanel.getHeight());

        //engine.getFrame().getRootPanel().setVisible(false);
        rootPanel.add(loadingPanel);
        rootPanel.revalidate();
        rootPanel.repaint();
        loadingPanel.setVisible(true);

        this.timer = new Timer(displayTimeMillis, new ActionListener() {
            boolean used = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                LoadState.this.tindex++;
                if (!this.used) {
                    if (LoadState.this.tindex > 10) {
                        this.used = true;
                    }
                    LoadState.this.tmpImage.getGraphics().drawImage(ImageUtils.getByIndex(ImageUtils.getLocalImage("assets/colors.png"), 1, 0), 0, 0, LoadState.this.getWidth(), LoadState.this.getHeight(), null);
                }
                if (LoadState.this.tindex == 12) {
                    LoadState.this.tindex = 0;
                }
                LoadState.this.repaint();
            }
        });
        this.timer.start();

        timer.setRepeats(false);
        timer.start();
    }

}
