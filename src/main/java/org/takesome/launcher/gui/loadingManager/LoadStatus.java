package org.takesome.launcher.gui.loadingManager;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.button.Button;
import org.takesome.kaylasEngine.gui.components.checkbox.Checkbox;
import org.takesome.kaylasEngine.gui.components.dropBox.DropBox;
import org.takesome.kaylasEngine.gui.components.label.Label;
import org.takesome.kaylasEngine.gui.components.sprite.SpriteAnimation;
import org.takesome.kaylasEngine.gui.loadingManager.LoadManagerAttributes;
import org.takesome.kaylasEngine.gui.loadingManager.LoadingManager;
import org.takesome.kaylasEngine.utils.DataInjector;
import org.takesome.kaylasEngine.utils.animation.AnimationManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.takesome.kaylasEngine.utils.FontUtils.hexToColor;

/**
 * Launcher-specific loading window implementation.
 *
 * <p>The class owns the visual loading overlay, sprite loader, progress animation, and
 * lightweight EDT diagnostics used to identify UI stalls while the loading layer is visible.</p>
 */
public class LoadStatus extends LoadingManager {
    private static final long UI_QUEUE_WARN_NANOS = 250_000_000L;
    private static final long SLOW_INIT_WARN_NANOS = 250_000_000L;

    private final ComponentsAccessor loadPanel, loggedForm;
    private final Launcher launcher;
    private final LoadingUiScriptConfig loadingUi;
    private final EdtLagWatchdog edtLagWatchdog = new EdtLagWatchdog("LoadStatus");
    private final List<SpriteAnimation> pausedFrameSprites = new ArrayList<>();

    private JProgressBar progressBar;
    private Label progressText;
    private JPanel loadingOverlay;
    private Timer overlayFadeTimer;
    private int overlayAlpha;
    private ProgressBarAnimator progressBarAnimator;

    /** Completes when Swing components required by the loading frame are initialized. */
    private final DataInjector<Boolean> initInjector = new DataInjector<>();

    public LoadStatus(Launcher launcher, int index) {
        super(launcher);
        this.launcher = launcher;
        this.loadingUi = LoadingUiScriptConfig.load(launcher);

        this.attributesList = List.of(this.engine.getEngineData().getLoadManager());
        this.loadingText = engine.getLANG().getString("loading.msg");
        this.loadingTitle = engine.getLANG().getString("loading.title");

        this.ANIMATION_SPEED = attributesList.get(index).getAnimSpeed();
        this.animationManager = new AnimationManager(this, getANIMATION_DURATION(), getANIMATION_SPEED());
        this.animationManager.setAnimationStats(this);

        this.loadPanel = new ComponentsAccessor(this.engine.getGuiBuilder(), "loadPanel", List.of(Label.class, SpriteAnimation.class, JProgressBar.class));
        this.loggedForm = new ComponentsAccessor(this.engine.getGuiBuilder(), "loggedForm", List.of(Button.class, DropBox.class, Checkbox.class));

        Engine.getLOGGER().info(
                "[LOAD-UI] config: animationSpeed={} overlayAlpha={} overlayFadeInMs={} overlayFadeOutMs={} overlayFrameDelayMs={} progressEnabled={}",
                ANIMATION_SPEED,
                loadingUi.overlay().targetAlpha(),
                loadingUi.overlay().fadeInMs(),
                loadingUi.overlay().fadeOutMs(),
                loadingUi.overlay().frameDelayMs(),
                loadingUi.progress().enabled()
        );

        long queuedAt = System.nanoTime();
        SwingUtilities.invokeLater(() -> {
            logUiQueueDelay("initializeLoadingFrame", queuedAt);
            initializeLoadingFrame(index);
        });
    }

    /**
     * Initializes the loading frame for the selected loading-manager attribute set.
     *
     * @param index loading-manager attribute index
     */
    @Override
    protected void initializeLoadingFrame(int index) {
        if (!SwingUtilities.isEventDispatchThread()) {
            long queuedAt = System.nanoTime();
            SwingUtilities.invokeLater(() -> {
                logUiQueueDelay("initializeLoadingFrame.reschedule", queuedAt);
                initializeLoadingFrame(index);
            });
            return;
        }

        long startedAt = System.nanoTime();
        try {
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
            setAlwaysOnTop(loadingUi.window().alwaysOnTop());

            int cornerRadius = loadingUi.window().cornerRadius();
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));

            initInjector.setContent(true);
        } catch (Exception ex) {
            Engine.getLOGGER().error("Unable to initialize loading frame", ex);
        } finally {
            long elapsed = System.nanoTime() - startedAt;
            if (elapsed >= SLOW_INIT_WARN_NANOS) {
                Engine.getLOGGER().warn("[LOAD-UI] initializeLoadingFrame took {} ms", nanosToMillis(elapsed));
            } else {
                Engine.getLOGGER().debug("[LOAD-UI] initializeLoadingFrame took {} ms", nanosToMillis(elapsed));
            }
        }
    }

    /**
     * Binds labels and progress widgets from the configured loading panel.
     *
     * @param attributes loading-manager visual attributes
     */
    private void setupLabels(LoadManagerAttributes attributes) {
        if (!SwingUtilities.isEventDispatchThread()) {
            long queuedAt = System.nanoTime();
            SwingUtilities.invokeLater(() -> {
                logUiQueueDelay("setupLabels.reschedule", queuedAt);
                setupLabels(attributes);
            });
            return;
        }

        try {
            titleLabel = (JLabel) loadPanel.getComponent("titleLabel");
            loaderText = (JLabel) loadPanel.getComponent("loaderText");

            Object progressComponent = loadPanel.getComponent("loadProgress");
            if (progressComponent instanceof JProgressBar) {
                progressBar = (JProgressBar) progressComponent;
            } else {
                Engine.getLOGGER().error("Component 'loadProgress' was not found or is not a JProgressBar.");
            }

            Object progressTextComponent = loadPanel.getComponent("progressText");
            if (progressTextComponent instanceof Label) {
                progressText = (Label) progressTextComponent;
            } else {
                Engine.getLOGGER().error("Component 'progressText' was not found or is not a Label.");
            }

            if (progressBar != null && progressText != null && progressText.getParent() != progressBar) {
                progressBar.add(progressText);
            }

            if (loaderText != null) {
                if (loadingText != null) {
                    loaderText.setText(loadingText);
                }
                loaderText.setForeground(hexToColor(attributes.getDescColor()));
            }
            if (titleLabel != null) {
                if (loadingTitle != null) {
                    titleLabel.setText(loadingTitle);
                }
                titleLabel.setForeground(hexToColor(attributes.getTitleColor()));
            }
        } catch (Exception ex) {
            Engine.getLOGGER().error("Unable to configure LoadStatus labels", ex);
        }
    }

    /** Kept for compatibility with the base floating-window contract. */
    @Override
    protected void initializeLoadingFrame() {
        // no-op
    }

    /** Shows the loading window and starts lightweight EDT stall monitoring. */
    @Override
    public void fadeIn() {
        long listenerRegisteredAt = System.nanoTime();
        initInjector.addListener(initialized -> {
            long queuedAt = System.nanoTime();
            SwingUtilities.invokeLater(() -> {
                logUiQueueDelay("fadeIn", queuedAt);
                try {
                    Engine.getLOGGER().info(
                            "[LOAD-UI] fadeIn requested; initWait={} ms",
                            nanosToMillis(System.nanoTime() - listenerRegisteredAt)
                    );
                    edtLagWatchdog.start();
                    pauseFrameSprites();
                    changeComponentStatus(loggedForm.getComponentMap(), loggedForm.getPanel(), false);
                    setVisible(true);
                    ensureOverlay();
                    fadeOverlayTo(loadingUi.overlay().targetAlpha(), loadingUi.overlay().fadeInMs(), null);
                    startProgressAnimator();
                } catch (Exception ex) {
                    Engine.getLOGGER().error("Unable to fade in loading overlay", ex);
                }
            });
        });
    }

    /** Hides the loading window and stops all loading-layer animations. */
    @Override
    public void fadeOut() {
        long queuedAt = System.nanoTime();
        SwingUtilities.invokeLater(() -> {
            logUiQueueDelay("fadeOut", queuedAt);
            try {
                Engine.getLOGGER().info("[LOAD-UI] fadeOut requested");
                if (progressBarAnimator != null) {
                    progressBarAnimator.stop();
                }
                if (progressBar != null) {
                    progressBar.setVisible(false);
                }
                fadeOverlayTo(0, loadingUi.overlay().fadeOutMs(), () -> {
                    removeOverlay();
                    setVisible(false);
                    resumeFrameSprites();
                    edtLagWatchdog.stop();
                    JPanel mainFramePanel = loggedForm.getPanel();
                    if (mainFramePanel != null) {
                        mainFramePanel.revalidate();
                        mainFramePanel.repaint();
                    }
                });
            } catch (Exception ex) {
                Engine.getLOGGER().error("Unable to fade out loading overlay", ex);
            }
        });
    }

    /** Creates the lightweight overlay used during loading transitions. */
    private void ensureOverlay() {
        JLayeredPane layeredPane = launcher.getFrame().getLayeredPane();
        if (loadingOverlay == null) {
            loadingOverlay = new JPanel() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    int alpha = Math.max(0, Math.min(255, overlayAlpha));
                    if (alpha <= 0) {
                        return;
                    }

                    Graphics2D graphics2D = (Graphics2D) graphics.create();
                    try {
                        graphics2D.setComposite(AlphaComposite.SrcOver.derive(alpha / 255f));
                        graphics2D.setColor(loadingUi.overlay().color());
                        graphics2D.fillRect(0, 0, getWidth(), getHeight());
                    } finally {
                        graphics2D.dispose();
                    }
                }
            };
            loadingOverlay.setName(loadingUi.overlay().name());
            loadingOverlay.setOpaque(false);
            loadingOverlay.setDoubleBuffered(true);
            Engine.getLOGGER().debug("[LOAD-UI] lightweight overlay created");
        }

        loadingOverlay.setBounds(loadingUi.overlay().bounds(launcher.getFrame().getWidth(), launcher.getFrame().getHeight()));
        if (loadingOverlay.getParent() != layeredPane) {
            layeredPane.add(loadingOverlay, JLayeredPane.POPUP_LAYER);
        }
        layeredPane.setLayer(loadingOverlay, JLayeredPane.POPUP_LAYER);
        loadingOverlay.setVisible(true);
        loadingOverlay.repaint();
    }

    private void removeOverlay() {
        if (loadingOverlay == null) {
            return;
        }
        Container parent = loadingOverlay.getParent();
        if (parent != null) {
            parent.remove(loadingOverlay);
            parent.revalidate();
            parent.repaint();
        }
        setOverlayAlpha(0);
    }

    /**
     * Applies the overlay alpha transition instantly.
     *
     * <p>The previous fade used a high-frequency Swing timer. During loading it competed with
     * sprite timers and progress timers. A single alpha update is materially cheaper and removes
     * a source of timer contention.</p>
     *
     * @param targetAlpha target alpha in the {@code 0..255} range
     * @param durationMs configured transition duration, logged for diagnostics only
     * @param onComplete optional completion callback executed on the EDT
     */
    private void fadeOverlayTo(int targetAlpha, int durationMs, Runnable onComplete) {
        ensureOverlay();
        if (overlayFadeTimer != null && overlayFadeTimer.isRunning()) {
            overlayFadeTimer.stop();
        }

        long startedAt = System.nanoTime();
        setOverlayAlpha(targetAlpha);
        Engine.getLOGGER().debug(
                "[LOAD-UI] overlay alpha applied instantly: targetAlpha={}, configuredDuration={} ms, elapsed={} ms",
                targetAlpha,
                durationMs,
                nanosToMillis(System.nanoTime() - startedAt)
        );
        if (onComplete != null) {
            onComplete.run();
        }
    }

    /** Starts the progress animation if it is enabled and all required widgets exist. */
    private void startProgressAnimator() {
        if (!loadingUi.progress().enabled() || progressBar == null || progressText == null) {
            Engine.getLOGGER().debug(
                    "[LOAD-UI] progress animator skipped: enabled={} progressBar={} progressText={}",
                    loadingUi.progress().enabled(),
                    progressBar != null,
                    progressText != null
            );
            return;
        }
        if (progressBarAnimator == null) {
            progressBarAnimator = new ProgressBarAnimator(launcher, progressBar, progressText);
        }
        progressBarAnimator.startProgressTest();
    }

    /** Pauses non-loading sprite animations from the main frame while the loading window is visible. */
    private void pauseFrameSprites() {
        pausedFrameSprites.clear();
        collectAndPauseSprites(launcher.getFrame().getContentPane());
        collectAndPauseSprites(launcher.getFrame().getLayeredPane());
        Engine.getLOGGER().debug("[LOAD-UI] paused frame sprites: count={}", pausedFrameSprites.size());
    }

    private void collectAndPauseSprites(Component component) {
        if (component instanceof SpriteAnimation spriteAnimation
                && !spriteAnimation.isAnimationStopped()
                && !pausedFrameSprites.contains(spriteAnimation)) {
            spriteAnimation.setAnimationStopped(true);
            pausedFrameSprites.add(spriteAnimation);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectAndPauseSprites(child);
            }
        }
    }

    /** Resumes only the sprite animations paused by {@link #pauseFrameSprites()}. */
    private void resumeFrameSprites() {
        for (SpriteAnimation spriteAnimation : pausedFrameSprites) {
            if (spriteAnimation.isDisplayable()) {
                spriteAnimation.setAnimationStopped(false);
            }
        }
        Engine.getLOGGER().debug("[LOAD-UI] resumed frame sprites: count={}", pausedFrameSprites.size());
        pausedFrameSprites.clear();
    }

    private void setOverlayAlpha(int alpha) {
        overlayAlpha = Math.max(0, Math.min(255, alpha));
        if (loadingOverlay != null) {
            loadingOverlay.repaint();
        }
    }

    private void logUiQueueDelay(String operation, long queuedAtNanos) {
        long delay = System.nanoTime() - queuedAtNanos;
        if (delay >= UI_QUEUE_WARN_NANOS) {
            Engine.getLOGGER().warn("[EDT-QUEUE] {} waited {} ms before execution", operation, nanosToMillis(delay));
        }
    }

    private static long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    /**
     * Small EDT heartbeat used while the loading UI is visible.
     *
     * <p>Because Swing timers execute on the EDT, a delayed heartbeat directly exposes UI thread stalls.</p>
     */
    private static final class EdtLagWatchdog {
        private static final int SAMPLE_MS = 250;
        private static final long LAG_WARN_NANOS = 350_000_000L;
        private static final long STATUS_INTERVAL_NANOS = 5_000_000_000L;

        private final String name;
        private final Timer timer;
        private long lastTickNanos;
        private long lastStatusNanos;
        private long maxLagNanos;
        private boolean running;

        private EdtLagWatchdog(String name) {
            this.name = name;
            this.timer = new Timer(SAMPLE_MS, event -> tick());
            this.timer.setCoalesce(true);
        }

        private void start() {
            if (running) {
                return;
            }
            running = true;
            lastTickNanos = System.nanoTime();
            lastStatusNanos = lastTickNanos;
            maxLagNanos = 0L;
            timer.start();
            Engine.getLOGGER().info("[EDT-WATCHDOG] {} started", name);
        }

        private void stop() {
            if (!running) {
                return;
            }
            timer.stop();
            running = false;
            Engine.getLOGGER().info("[EDT-WATCHDOG] {} stopped; maxLag={} ms", name, nanosToMillis(maxLagNanos));
        }

        private void tick() {
            long now = System.nanoTime();
            long elapsed = now - lastTickNanos;
            long expected = SAMPLE_MS * 1_000_000L;
            long lag = Math.max(0L, elapsed - expected);
            maxLagNanos = Math.max(maxLagNanos, lag);

            if (lag >= LAG_WARN_NANOS) {
                Engine.getLOGGER().warn("[EDT-LAG] {} delayed by {} ms", name, nanosToMillis(lag));
            } else if (now - lastStatusNanos >= STATUS_INTERVAL_NANOS) {
                Engine.getLOGGER().debug("[EDT-WATCHDOG] {} alive; maxLag={} ms", name, nanosToMillis(maxLagNanos));
                lastStatusNanos = now;
            }

            lastTickNanos = now;
        }
    }
}
