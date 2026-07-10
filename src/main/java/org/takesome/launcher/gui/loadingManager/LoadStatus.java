package org.takesome.launcher.gui.loadingManager;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.animation.LayeredPaneOverlay;
import org.takesome.kaylasEngine.gui.animation.ScriptedWindowAnimator;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.ComponentFactory;
import org.takesome.kaylasEngine.gui.components.button.Button;
import org.takesome.kaylasEngine.gui.components.checkbox.Checkbox;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.label.Label;
import org.takesome.kaylasEngine.gui.components.progressBar.ProgressBar;
import org.takesome.kaylasEngine.gui.components.progressBar.ProgressBarStyle;
import org.takesome.kaylasEngine.gui.components.sprite.SpriteAnimation;
import org.takesome.kaylasEngine.gui.diagnostics.EdtLagWatchdog;
import org.takesome.kaylasEngine.gui.loadingManager.LoadingManager;
import org.takesome.kaylasEngine.gui.loadingManager.ScriptedLoadingUi;
import org.takesome.kaylasEngine.gui.styles.StyleAttributes;
import org.takesome.kaylasEngine.utils.DataInjector;
import org.takesome.launcher.gui.LauncherUiProvider;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

import static org.takesome.kaylasEngine.utils.FontUtils.hexToColor;
import static org.takesome.kaylasEngine.utils.FontUtils.styleName;

/**
 * Launcher binding for the engine-owned scripted loading UI runtime.
 *
 * <p>Every visual and animation decision lives in the application Lua script. This class only
 * binds launcher components, localization and lifecycle events to generic engine services.</p>
 */
public class LoadStatus extends LoadingManager {
    private static final long UI_QUEUE_WARN_NANOS = 250_000_000L;
    private static final long SLOW_INIT_WARN_NANOS = 250_000_000L;

    private final ComponentsAccessor loadPanel, loggedForm;
    private final Launcher launcher;
    private final ScriptedLoadingUi loadingUi;
    private final ScriptedWindowAnimator scriptedWindowAnimator;
    private final EdtLagWatchdog edtLagWatchdog = new EdtLagWatchdog("LoadStatus", Engine.getLOGGER());

    private ProgressBar progressBar;
    private JLabel progressText;
    private ProgressBarAnimator progressBarAnimator;
    private LayeredPaneOverlay overlayController;

    /** Completes when Swing components required by the loading frame are initialized. */
    private final DataInjector<Boolean> initInjector = new DataInjector<>();

    public LoadStatus(Launcher launcher, int profileIndex) {
        super(launcher);
        this.launcher = launcher;
        this.loadingUi = ScriptedLoadingUi.load(
                launcher.getGuiBuilder()
                        .getComponentFactory()
                        .getLuaUiScriptEngine()
                        .getContext(),
                LauncherUiProvider.load().loadingUiScriptPath(),
                profileIndex
        );
        this.FRAME_WIDTH = loadingUi.window().width();
        this.FRAME_HEIGHT = loadingUi.window().height();
        this.scriptedWindowAnimator = new ScriptedWindowAnimator(this, loadingUi.transition());
        this.scriptedWindowAnimator.setAnimationStats(this);

        this.loadingText = engine.getLANG().getString("loading.msg");
        this.loadingTitle = engine.getLANG().getString("loading.title");

        this.loadPanel = new ComponentsAccessor(
                this.engine.getGuiBuilder(),
                "loadPanel",
                List.of(Label.class, SpriteAnimation.class, ProgressBar.class)
        );
        this.loggedForm = new ComponentsAccessor(
                this.engine.getGuiBuilder(),
                "loggedForm",
                List.of(Button.class, Combobox.class, Checkbox.class)
        );

        Engine.getLOGGER().info(
                "[LOAD-UI] script policy: window={}x{} loaderEnabled={} sprite={} grid={}x{} spriteFrameMs={} loaderBounds={} transitionEnabled={} entryMotionMs={} entryOpacityMs={} exitMotionMs={} exitOpacityMs={} overlayEnabled={} overlayAlpha={} overlayFadeInMs={} overlayFadeOutMs={} overlayBounds={} progressEnabled={} progressUpdateMs={} progressStep={} progressLoop={} progressTimelineMs={} progressTimelineFrameMs={} randomMessages={} showText={} showPercent={} messagesSection={} localizedMessages={} progressStyle={} progressFont={} progressFontSize={} progressFontStyle={} titleStyle={} messageStyle={}",
                loadingUi.window().width(),
                loadingUi.window().height(),
                loadingUi.loader().enabled(),
                loadingUi.loader().spritePath(),
                loadingUi.loader().rows(),
                loadingUi.loader().columns(),
                loadingUi.loader().frameDelayMs(),
                loadingUi.loader().bounds(),
                loadingUi.transition().enabled(),
                loadingUi.transition().entry().motion().totalDurationMs(),
                loadingUi.transition().entry().opacity().totalDurationMs(),
                loadingUi.transition().exit().motion().totalDurationMs(),
                loadingUi.transition().exit().opacity().totalDurationMs(),
                loadingUi.overlay().enabled(),
                loadingUi.overlay().targetAlpha(),
                loadingUi.overlay().fadeIn().durationMs(),
                loadingUi.overlay().fadeOut().durationMs(),
                loadingUi.overlay().bounds(launcher.getFrame().getWidth(), launcher.getFrame().getHeight()),
                loadingUi.progress().enabled(),
                loadingUi.progress().updateMs(),
                loadingUi.progress().step(),
                loadingUi.progress().loop(),
                loadingUi.progress().timelineDurationMs(),
                loadingUi.progress().timelineFrameDelayMs(),
                loadingUi.progress().randomMessages(),
                loadingUi.progress().showText(),
                loadingUi.progress().showPercent(),
                loadingUi.progress().messagesSection(),
                launcher.getLANG().getSectionValues(loadingUi.progress().messagesSection()).size(),
                loadingUi.progress().styleName(),
                loadingUi.progress().fontName(),
                loadingUi.progress().fontSize(),
                loadingUi.progress().fontStyle(),
                loadingUi.typography().title().styleName(),
                loadingUi.typography().message().styleName()
        );

        long queuedAt = System.nanoTime();
        SwingUtilities.invokeLater(() -> {
            logUiQueueDelay("initializeLoadingFrame", queuedAt);
            initializeLoadingFrame(profileIndex);
        });
    }

    @Override
    protected void initializeLoadingFrame(int ignoredProfileIndex) {
        if (!SwingUtilities.isEventDispatchThread()) {
            long queuedAt = System.nanoTime();
            SwingUtilities.invokeLater(() -> {
                logUiQueueDelay("initializeLoadingFrame.reschedule", queuedAt);
                initializeLoadingFrame(ignoredProfileIndex);
            });
            return;
        }

        long startedAt = System.nanoTime();
        try {
            setSize(loadingUi.window().width(), loadingUi.window().height());
            ScriptedLoadingUi.Loader loaderConfig = loadingUi.loader();

            createBackgroundPanel(
                    this.engine.getGuiBuilder().getPanelsMap().get("loadPanel"),
                    loaderConfig.background().image(),
                    loaderConfig.background().color()
            );

            if (loaderConfig.enabled() && !loaderConfig.spritePath().isBlank()) {
                Rectangle loaderBounds = loaderConfig.bounds();
                SpriteAnimation currentLoader = new SpriteAnimation(
                        engine,
                        loaderConfig.spritePath(),
                        loaderConfig.rows(),
                        loaderConfig.columns(),
                        loaderConfig.frameDelayMs(),
                        new Rectangle(loaderBounds)
                );
                currentLoader.setBounds(loaderBounds);
                backgroundPanel.add(currentLoader);
            }
            backgroundPanel.setVisible(true);

            setupLabels(loaderConfig);
            setAlwaysOnTop(loadingUi.window().alwaysOnTop());

            int cornerRadius = loadingUi.window().cornerRadius();
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
            initInjector.setContent(true);
        } catch (Exception error) {
            Engine.getLOGGER().error("Unable to initialize scripted loading frame", error);
        } finally {
            long elapsed = System.nanoTime() - startedAt;
            if (elapsed >= SLOW_INIT_WARN_NANOS) {
                Engine.getLOGGER().warn("[LOAD-UI] initializeLoadingFrame took {} ms", nanosToMillis(elapsed));
            } else {
                Engine.getLOGGER().debug("[LOAD-UI] initializeLoadingFrame took {} ms", nanosToMillis(elapsed));
            }
        }
    }

    private void setupLabels(ScriptedLoadingUi.Loader loaderConfig) {
        if (!SwingUtilities.isEventDispatchThread()) {
            long queuedAt = System.nanoTime();
            SwingUtilities.invokeLater(() -> {
                logUiQueueDelay("setupLabels.reschedule", queuedAt);
                setupLabels(loaderConfig);
            });
            return;
        }

        try {
            titleLabel = (JLabel) loadPanel.getComponent("titleLabel");
            loaderText = (JLabel) loadPanel.getComponent("loaderText");

            Object progressComponent = loadPanel.getComponent("loadProgress");
            if (progressComponent instanceof ProgressBar compositeProgressBar) {
                progressBar = compositeProgressBar;
                ComponentFactory componentFactory = engine.getGuiBuilder().getComponentFactory();
                String appliedStyle = ProgressBarStyle.applyNamedStyle(
                        componentFactory,
                        progressBar,
                        loadingUi.progress().styleName()
                );
                applyProgressTypography(progressBar, loadingUi.progress());
                progressBar.setStringPainted(loadingUi.progress().showText());
                progressBar.setShowPercent(loadingUi.progress().showPercent());
                progressText = compositeProgressBar.getTextLabel();
                Engine.getLOGGER().debug(
                        "[LOAD-UI] progress visual policy applied: style={} font={} size={} fontStyle={} textColor={}",
                        appliedStyle,
                        progressBar.getTextFont().getFamily(),
                        progressBar.getTextFont().getSize(),
                        styleName(progressBar.getTextFont().getStyle()),
                        progressBar.getTextColor()
                );
            } else {
                Engine.getLOGGER().error("Component 'loadProgress' was not found or is not a composite ProgressBar.");
            }

            if (loaderText != null) {
                if (loadingText != null) {
                    loaderText.setText(loadingText);
                }
                applyLabelTypography(
                        loaderText,
                        loadingUi.typography().message(),
                        loaderConfig.messageColor()
                );
            }
            if (titleLabel != null) {
                if (loadingTitle != null) {
                    titleLabel.setText(loadingTitle);
                }
                applyLabelTypography(
                        titleLabel,
                        loadingUi.typography().title(),
                        loaderConfig.titleColor()
                );
            }
        } catch (Exception error) {
            Engine.getLOGGER().error("Unable to configure LoadStatus labels", error);
        }
    }

    private void applyProgressTypography(ProgressBar progressBar,
                                         ScriptedLoadingUi.Progress progressConfig) {
        Font currentFont = progressBar.getTextFont();
        String fontName = valueOr(progressConfig.fontName(), currentFont.getFamily());
        int fontSize = progressConfig.fontSize() > 0
                ? progressConfig.fontSize()
                : currentFont.getSize();
        String fontStyle = valueOr(progressConfig.fontStyle(), styleName(currentFont.getStyle()));
        progressBar.setTextFont(engine.getFONTUTILS().getFont(fontName, fontSize, fontStyle));
        if (progressConfig.textColor() != null && !progressConfig.textColor().isBlank()) {
            progressBar.setTextColor(hexToColor(progressConfig.textColor()));
        }
    }

    private void applyLabelTypography(JLabel label,
                                      ScriptedLoadingUi.TextStyle textConfig,
                                      String fallbackColor) {
        String requestedStyle = valueOr(textConfig.styleName(), "default");
        StyleAttributes style = engine.getStyleProvider().getStyle("label", requestedStyle);
        String fontName = valueOr(textConfig.fontName(), style.getFont());
        int fontSize = textConfig.fontSize() > 0 ? textConfig.fontSize() : style.getFontSize();
        String fontStyle = valueOr(textConfig.fontStyle(), style.getFontStyle());
        String color = valueOr(textConfig.color(), valueOr(fallbackColor, style.getColor()));

        label.setFont(engine.getFONTUTILS().getFont(fontName, fontSize, fontStyle));
        label.setForeground(hexToColor(color));
        label.setHorizontalAlignment(textAlignment(style.getAlign()));
        label.putClientProperty("kaylas.ui.label.style", requestedStyle);
    }

    private int textAlignment(String alignment) {
        if (alignment == null) {
            return JLabel.LEFT;
        }
        return switch (alignment.trim().toLowerCase()) {
            case "center", "middle" -> JLabel.CENTER;
            case "right", "end" -> JLabel.RIGHT;
            default -> JLabel.LEFT;
        };
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Override
    protected void initializeLoadingFrame() {
        // Compatibility with the base floating-window contract.
    }

    @Override
    public void animateLoadingWindow(boolean isEntry) {
        scriptedWindowAnimator.animate(isEntry);
    }

    @Override
    public void toggleVisibility() {
        scriptedWindowAnimator.toggleVisibility();
    }

    @Override
    protected void updateLoadingFramePosition() {
        long queuedAt = System.nanoTime();
        SwingUtilities.invokeLater(() -> {
            logUiQueueDelay("updateLoadingFramePosition", queuedAt);
            if (!isAnimating()) {
                Point target = loadingUi.transition()
                        .entry()
                        .motion()
                        .to()
                        .resolve(this, getLocation());
                setLocation(target);
            }
            if (overlayController != null) {
                overlayController.refreshBounds();
            }
        });
    }

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
                    changeComponentStatus(loggedForm.getComponentMap(), loggedForm.getPanel(), false);
                    setVisible(true);
                    if (loadingUi.overlay().enabled()) {
                        ScriptedLoadingUi.Fade fade = loadingUi.overlay().fadeIn();
                        overlayController().fadeIn(
                                loadingUi.overlay().targetAlpha(),
                                fade.durationMs(),
                                fade.frameDelayMs(),
                                fade.curve(),
                                null
                        );
                    }
                    startProgressAnimator();
                } catch (Exception error) {
                    Engine.getLOGGER().error("Unable to fade in scripted loading UI", error);
                }
            });
        });
    }

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

                Runnable complete = this::completeFadeOut;
                if (loadingUi.overlay().enabled() && overlayController != null) {
                    ScriptedLoadingUi.Fade fade = loadingUi.overlay().fadeOut();
                    overlayController.fadeOut(
                            fade.durationMs(),
                            fade.frameDelayMs(),
                            fade.curve(),
                            complete
                    );
                } else {
                    complete.run();
                }
            } catch (Exception error) {
                Engine.getLOGGER().error("Unable to fade out scripted loading UI", error);
                completeFadeOut();
            }
        });
    }

    private void completeFadeOut() {
        setVisible(false);
        edtLagWatchdog.stop();
        JPanel mainFramePanel = loggedForm.getPanel();
        if (mainFramePanel != null) {
            mainFramePanel.revalidate();
            mainFramePanel.repaint();
        }
    }

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
            progressBarAnimator = new ProgressBarAnimator(launcher, progressBar, progressText, loadingUi.progress());
        }
        progressBarAnimator.startProgressTest();
    }

    private LayeredPaneOverlay overlayController() {
        if (overlayController == null) {
            int defaultFrameDelayMs = Math.min(
                    loadingUi.overlay().fadeIn().frameDelayMs(),
                    loadingUi.overlay().fadeOut().frameDelayMs()
            );
            overlayController = new LayeredPaneOverlay(
                    launcher.getFrame().getLayeredPane(),
                    () -> loadingUi.overlay().bounds(
                            launcher.getFrame().getWidth(),
                            launcher.getFrame().getHeight()
                    ),
                    loadingUi.overlay().color(),
                    loadingUi.overlay().name(),
                    defaultFrameDelayMs,
                    Engine.getLOGGER(),
                    "[LOAD-UI]"
            );
        }
        return overlayController;
    }

    private void logUiQueueDelay(String operation, long queuedAtNanos) {
        edtLagWatchdog.logQueueDelay(operation, queuedAtNanos, UI_QUEUE_WARN_NANOS);
    }

    private static long nanosToMillis(long nanos) {
        return EdtLagWatchdog.nanosToMillis(nanos);
    }
}
