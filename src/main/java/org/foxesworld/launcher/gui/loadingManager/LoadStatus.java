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
import java.util.concurrent.ExecutionException;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class LoadStatus extends LoadingManager implements AnimationStats {
    private final ComponentsAccessor componentsAccessor;

    public LoadStatus(Engine engine, int index) {
        this.engine = engine;
        this.attributesList = List.of(engine.getEngineData().getLoadManager());
        this.loadingText = engine.getLANG().getString("loading.msg");
        this.loadingTitle = engine.getLANG().getString("loading.title");

        this.ANIMATION_SPEED = attributesList.get(index).getAnimSpeed();
        this.animationManager = new AnimationManager(this, getANIMATION_DURATION(), getANIMATION_SPEED());
        this.animationManager.setAnimationStats(this);
        this.componentsAccessor = new ComponentsAccessor(this.engine.getGuiBuilder(), "loadPanel", List.of(Label.class, SpriteAnimation.class));

        new InitializationWorker(index).execute();
    }

    @Override
    protected void initializeLoadingFrame(int index) {
        SwingUtilities.invokeLater(() -> {
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

    private class InitializationWorker extends SwingWorker<Void, Void> {
        private final int index;

        public InitializationWorker(int index) {
            this.index = index;
        }

        @Override
        protected Void doInBackground() {
            initializeLoadingFrame(index);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }
    }
}