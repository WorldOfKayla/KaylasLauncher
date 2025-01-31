package org.foxesworld.launcher.gui.loadingManager;

import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.engine.gui.loadingManager.LoadManagerAttributes;
import org.foxesworld.engine.gui.loadingManager.LoadingManager;
import org.foxesworld.engine.utils.animation.AnimationManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class LoadStatus extends LoadingManager {
    private final ComponentsAccessor loadPanel, loggedForm;
    private final Launcher launcher;

    public LoadStatus(Launcher launcher, int index) {
        super(launcher);
        this.launcher = launcher;
        this.attributesList = List.of(this.engine.getEngineData().getLoadManager());
        this.loadingText = engine.getLANG().getString("loading.msg");
        this.loadingTitle = engine.getLANG().getString("loading.title");
        this.ANIMATION_SPEED = attributesList.get(index).getAnimSpeed();
        this.animationManager = new AnimationManager(this, getANIMATION_DURATION(), getANIMATION_SPEED());
        this.animationManager.setAnimationStats(this);
        this.loadPanel = new ComponentsAccessor(this.engine.getGuiBuilder(), "loadPanel", List.of(Label.class, SpriteAnimation.class, JProgressBar.class));
        this.loggedForm = new ComponentsAccessor(this.engine.getGuiBuilder(), "loggedForm", List.of(Button.class, DropBox.class, Checkbox.class));
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                initializeLoadingFrame(index);
                return null;
            }
        }.execute();
    }

    @Override
    protected void initializeLoadingFrame(int index) {
        SwingUtilities.invokeLater(() -> {
            setSize(getFrameWidth(), getFrameHeight());
            LoadManagerAttributes attributes = attributesList.get(index);
            createBackgroundPanel(
                    this.engine.getGuiBuilder().getPanelsMap().get("loadPanel"),
                    attributes.getBgPath(),
                    attributes.getBlurColor()
            );

            SpriteAnimation currentLoader = new SpriteAnimation(
                    engine,
                    attributes.getSpritePath(),
                    attributes.getRows(),
                    attributes.getCols(),
                    attributes.getDelay(),
                    new Rectangle(attributes.getBounds())
            );

            currentLoader.setBounds(attributes.getBounds());
            backgroundPanel.setVisible(true);
            backgroundPanel.add(currentLoader);
            setupLabels(attributes);
            setAlwaysOnTop(true);
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
        });
    }

    private void setupLabels(LoadManagerAttributes attributes) {
        SwingUtilities.invokeLater(() -> {
            titleLabel = (JLabel) loadPanel.getComponent("titleLabel");
            loaderText = (JLabel) loadPanel.getComponent("loaderText");
            loaderText.setForeground(hexToColor(attributes.getDescColor()));
            titleLabel.setForeground(hexToColor(attributes.getTitleColor()));
        });
    }

    @Override
    protected void initializeLoadingFrame() {
    }

    @Override
    public void fadeIn() {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            changeComponentStatus(loggedForm.getComponentMap(), loggedForm.getPanel(), false);
            SwingUtilities.invokeLater(() -> {
                this.setVisible(true);
                JLayeredPane layeredPane = launcher.getFrame().getLayeredPane();
                JPanel overlay = getOverlay();
                overlay.setBackground(new Color(0, 0, 0, 0));
                overlay.setName("loadingOverlay");
                layeredPane.add(overlay, JLayeredPane.POPUP_LAYER);
                overlay.setBounds(0, 0, launcher.getFrame().getWidth(), launcher.getFrame().getHeight());

                AtomicInteger alpha = new AtomicInteger(0);
                Timer timer = new Timer(30, e -> {
                    alpha.addAndGet(10);
                    overlay.setBackground(new Color(0, 0, 0, Math.min(alpha.get(), 180)));
                    overlay.repaint();
                    if (alpha.get() >= 180) {
                        ((Timer) e.getSource()).stop();
                    }
                });
                timer.start();
            });
        }, "fadeIn");
    }

    @Override
    public void fadeOut() {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            JLayeredPane layeredPane = launcher.getFrame().getLayeredPane();
            JPanel mainFramePanel = loggedForm.getPanel();

            SwingUtilities.invokeLater(() -> {
                for (Component component : layeredPane.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
                    if (component instanceof JPanel overlay && "loadingOverlay".equals(overlay.getName())) {
                        AtomicInteger alpha = new AtomicInteger(overlay.getBackground().getAlpha());
                        Timer timer = new Timer(30, e -> {
                            alpha.addAndGet(-10);
                            overlay.setBackground(new Color(0, 0, 0, Math.max(alpha.get(), 0)));
                            overlay.repaint();
                            if (alpha.get() <= 0) {
                                ((Timer) e.getSource()).stop();
                                layeredPane.remove(overlay);
                                layeredPane.revalidate();
                                layeredPane.repaint();
                            }
                        });
                        timer.start();
                    }
                }
                this.setVisible(false);
                mainFramePanel.revalidate();
                mainFramePanel.repaint();
            });
        }, "fadeOut");
    }
}
