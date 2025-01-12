package org.foxesworld.launcher.gui.loadingManager;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.engine.gui.loadingManager.LoadManagerAttributes;
import org.foxesworld.engine.gui.loadingManager.LoadingManager;
import org.foxesworld.engine.utils.animation.AnimationManager;
import org.foxesworld.engine.utils.animation.AnimationStats;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class LoadStatus extends LoadingManager implements AnimationStats {
    private final ComponentsAccessor componentsAccessor;
    //private final JProgressBar progressBar;

    public LoadStatus(Engine engine, int index) {
        this.engine = engine;
        this.attributesList = List.of(engine.getEngineData().getLoadManager());
        this.loadingText = engine.getLANG().getString("loading.msg");
        this.loadingTitle = engine.getLANG().getString("loading.title");

        this.ANIMATION_SPEED = attributesList.get(index).getAnimSpeed();
        this.animationManager = new AnimationManager(this, getANIMATION_DURATION(), getANIMATION_SPEED());
        this.animationManager.setAnimationStats(this);
        this.componentsAccessor = new ComponentsAccessor(this.engine.getGuiBuilder(), "loadPanel", List.of(Label.class, SpriteAnimation.class, JProgressBar.class));
        //this.progressBar = (JProgressBar) this.componentsAccessor.getComponent("loadProgress");
        //this.progressBar.setMinimum(100);
        this.engine.getExecutorServiceProvider().submitTask(() -> this.initializeLoadingFrame(index), "initializeLoadingFrame");
    }

    @Override
    protected void initializeLoadingFrame(int index) {
        SwingUtilities.invokeLater(() -> {
            //this.progressBar.setValue(0);
            setSize(getFrameWidth(), getFrameHeight());
            LoadManagerAttributes attributes = attributesList.get(index);

            JPanel backgroundPanel = createBackgroundPanel(
                    this.engine.getGuiBuilder().getPanelsMap().get("loadPanel"),
                    attributes.getBgPath(),
                    attributes.getBlurColor()
            );

            backgroundPanel.setVisible(true);
            SpriteAnimation currentLoader = new SpriteAnimation(
                    engine,
                    attributes.getSpritePath(),
                    attributes.getRows(),
                    attributes.getCols(),
                    attributes.getDelay(),
                    new Rectangle(attributes.getBounds())
            );

            currentLoader.setBounds(attributes.getBounds());
            setContentPane(backgroundPanel);
            backgroundPanel.add(currentLoader);

            setupLabels(attributes);

            setAlwaysOnTop(true);
            setLocationRelativeTo(engine.getFrame());
            addFrameComponentListener();
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
        });
    }

    private void setupLabels(LoadManagerAttributes attributes) {
        SwingUtilities.invokeLater(() -> {
            titleLabel = (JLabel) componentsAccessor.getComponent("titleLabel");
            loaderText = (JLabel) componentsAccessor.getComponent("loaderText");
            loaderText.setForeground(hexToColor(attributes.getDescColor()));
            titleLabel.setForeground(hexToColor(attributes.getTitleColor()));
        });
    }

    @Override
    public void animationStarted() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }

    @Override
    public void animationFinished() {
        SwingUtilities.invokeLater(() -> this.setVisible(false));
    }

    //public void setProgress(int value){ this.progressBar.setValue(value); }

    //public JProgressBar getProgressBar() {return progressBar;}
}