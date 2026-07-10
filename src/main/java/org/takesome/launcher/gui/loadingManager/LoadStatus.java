package org.takesome.launcher.gui.loadingManager;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.animation.LayeredPaneOverlay;
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
import org.takesome.kaylasEngine.gui.loadingManager.LoadManagerAttributes;
import org.takesome.kaylasEngine.gui.loadingManager.LoadingManager;
import org.takesome.kaylasEngine.gui.styles.StyleAttributes;
import org.takesome.kaylasEngine.utils.DataInjector;
import org.takesome.kaylasEngine.utils.animation.AnimationManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

import static org.takesome.kaylasEngine.utils.FontUtils.hexToColor;
import static org.takesome.kaylasEngine.utils.FontUtils.styleName;

/**
 * Launcher-specific loading window implementation.
 *
 * <p>Visual primitives, overlay animation, progress animation, and EDT diagnostics are supplied by
 * KaylasUIEngine. This class only binds launcher components and localization to those services.</p>
 */
public class LoadStatus extends LoadingManager {
    private static final long UI_QUEUE_WARN_NANOS = 250_000_000L;
    private static final long SLOW_INIT_WARN_NANOS = 250_000_000L;

    private final ComponentsAccessor loadPanel, loggedForm;
    private final Launcher launcher;
    private final LoadingUiScriptConfig loadingUi;
    private final EdtLagWatchdog edtLagWatchdog = new EdtLagWatchdog("LoadStatus", Engine.getLOGGER());

    private ProgressBar progressBar;
    private JLabel progressText;
    private ProgressBarAnimator progressBarAnimator;
    private LayeredPaneOverlay overlayController;

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
                "[LOAD-UI] config: animationSpeed={} overlayAlpha={} overlayFadeInMs={} overlayFadeOutMs={} overlayFrameDelayMs={} progressEnabled={} progressUpdateMs={} progressStep={} progressLoop={} progressTimelineMs={} progressTimelineFrameMs={} randomMessages={} showText={} showPercent={} messagesSection={} localizedMessages={} progressStyle={} progressFont={} progressFontSize={} progressFontStyle={} titleStyle={} messageStyle={}",
                ANIMATION_SPEED,
                loadingUi.overlay().targetAlpha(),
                loadingUi.overlay().fadeInMs(),
                loadingUi.overlay().fadeOutMs(),
                loadingUi.overlay().frameDelayMs(),
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
            initializeLoadingFrame(index);
        });
    }

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
        } catch (Exception error) {
            Engine.getLOGGER().error("Unable to initialize loading frame", error);
        } finally {
            long elapsed = System.nanoTime() - startedAt;
            if (elapsed >= SLOW_INIT_WARN_NANOS) {
                Engine.getLOGGER().warn("[LOAD-UI] initializeLoadingFrame took {} ms", nanosToMillis(elapsed));
            } else {
                Engine.getLOGGER().debug("[LOAD-UI] initializeLoadingFrame took {} ms", nanosToMillis(elapsed));
            }
        }
    }

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
                        attributes.getDescColor()
                );
            }
            if (titleLabel != null) {
                if (loadingTitle != null) {
                    titleLabel.setText(loadingTitle);
                }
                applyLabelTypography(
                        titleLabel,
                        loadingUi.typography().title(),
                        attributes.getTitleColor()
                );
            }
        } catch (Exception error) {
            Engine.getLOGGER().error("Unable to configure LoadStatus labels", error);
        }
    }

    private void applyProgressTypography(ProgressBar progressBar,
                                         LoadingUiScriptConfig.Progress progressConfig) {
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
                                      LoadingUiScriptConfig.TextStyle textConfig,
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
                    overlayController().fadeIn(
                            loadingUi.overlay().targetAlpha(),
                            loadingUi.overlay().fadeInMs(),
                            null
                    );
                    startProgressAnimator();
                } catch (Exception error) {
                    Engine.getLOGGER().error("Unable to fade in loading overlay", error);
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
                overlayController().fadeOut(loadingUi.overlay().fadeOutMs(), () -> {
                    setVisible(false);
                    edtLagWatchdog.stop();
                    JPanel mainFramePanel = loggedForm.getPanel();
                    if (mainFramePanel != null) {
                        mainFramePanel.revalidate();
                        mainFramePanel.repaint();
                    }
                });
            } catch (Exception error) {
                Engine.getLOGGER().error("Unable to fade out loading overlay", error);
            }
        });
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
            overlayController = new LayeredPaneOverlay(
                    launcher.getFrame().getLayeredPane(),
                    () -> loadingUi.overlay().bounds(
                            launcher.getFrame().getWidth(),
                            launcher.getFrame().getHeight()
                    ),
                    loadingUi.overlay().color(),
                    loadingUi.overlay().name(),
                    loadingUi.overlay().frameDelayMs(),
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
