package org.foxesworld.launcher.gui.loadingManager;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.sprite.SpriteAnimation;
import org.foxesworld.engine.gui.loadingManager.LoadManagerAttributes;
import org.foxesworld.engine.gui.loadingManager.LoadingManager;
import org.foxesworld.engine.utils.DataInjector;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.animation.AnimationManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class LoadStatus extends LoadingManager {
    private final ComponentsAccessor loadPanel, loggedForm;
    private final Launcher launcher;
    private JProgressBar progressBar;
    private Label progressText;

    // DataInjector для уведомления о завершении инициализации
    private final DataInjector<Boolean> initInjector = new DataInjector<>();

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

            @Override
            protected void done() {
                // Дополнительные действия после выполнения SwingWorker, если необходимо
            }
        }.execute();
    }

    /**
     * Метод инициализирует окно загрузки с использованием заданного набора атрибутов.
     *
     * @param index индекс настроек загрузчика.
     */
    @Override
    protected void initializeLoadingFrame(int index) {
        SwingUtilities.invokeLater(() -> {
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

                // Настройка меток (заголовок, описание) с динамической цветовой схемой
                setupLabels(attributes);
                setAlwaysOnTop(true);
                // Задание формы окна с закруглёнными углами
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

                // Уведомляем слушателей, что инициализация завершена
                initInjector.setContent(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * Метод настраивает текстовые метки экрана загрузки в соответствии с переданными атрибутами.
     *
     * @param attributes атрибуты, содержащие цветовую схему и другие параметры.
     */
    private void setupLabels(LoadManagerAttributes attributes) {
        SwingUtilities.invokeLater(() -> {
            try {
                titleLabel = (JLabel) loadPanel.getComponent("titleLabel");
                loaderText = (JLabel) loadPanel.getComponent("loaderText");

                Object progressComponent = loadPanel.getComponent("loadProgress");
                if (progressComponent instanceof JProgressBar) {
                    progressBar = (JProgressBar) progressComponent;
                } else {
                    Engine.getLOGGER().error("Компонент 'loadProgress' не найден или не является JProgressBar.");
                }

                Object progressTextComponent = loadPanel.getComponent("progressText");
                if (progressTextComponent instanceof Label) {
                    progressText = (Label) progressTextComponent;
                } else {
                    Engine.getLOGGER().error("Компонент 'progressText' не найден или не является Label.");
                }

                if (progressBar != null && progressText != null) {
                    progressBar.add(progressText);
                }

                if (loaderText != null) {
                    loaderText.setForeground(hexToColor(attributes.getDescColor()));
                }
                if (titleLabel != null) {
                    titleLabel.setForeground(hexToColor(attributes.getTitleColor()));
                }
            } catch (Exception ex) {
                Engine.getLOGGER().error("Ошибка при настройке меток в LoadStatus", ex);
            }
        });
    }

    /**
     * Переопределённый метод (без реализации), оставленный для совместимости с базовым классом.
     */
    @Override
    protected void initializeLoadingFrame() {
        // no-op
    }

    /**
     * Метод выполняет анимацию появления (fadeIn) экрана загрузки.
     * Для периодических вычислений создаётся локальный ScheduledExecutorService.
     */
    @Override
    public void fadeIn() {
        initInjector.addListener(initialized -> {
            launcher.getExecutorServiceProvider().submitTask(() -> {
                try {
                    changeComponentStatus(loggedForm.getComponentMap(), loggedForm.getPanel(), false);

                    SwingUtilities.invokeLater(() -> {
                        setVisible(true);
                        JLayeredPane layeredPane = launcher.getFrame().getLayeredPane();
                        JPanel overlay = getOverlay();
                        overlay.setBackground(new Color(0, 0, 0, 0));
                        overlay.setName("loadingOverlay");
                        layeredPane.add(overlay, JLayeredPane.POPUP_LAYER);
                        overlay.setBounds(0, 0, launcher.getFrame().getWidth(), launcher.getFrame().getHeight());
                    });

                    // Создаём локальный scheduler для анимации
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                    AtomicInteger alpha = new AtomicInteger(0);
                    AtomicReference<ScheduledFuture<?>> fadeFutureRef = new AtomicReference<>();
                    fadeFutureRef.set(scheduler.scheduleAtFixedRate(() -> {
                        int newAlpha = Math.min(alpha.addAndGet(10), 180);
                        SwingUtilities.invokeLater(() -> {
                            JLayeredPane layeredPane = launcher.getFrame().getLayeredPane();
                            for (Component comp : layeredPane.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
                                if (comp instanceof JPanel && "loadingOverlay".equals(comp.getName())) {
                                    comp.setBackground(new Color(0, 0, 0, newAlpha));
                                    comp.repaint();
                                }
                            }
                        });
                        if (newAlpha >= 180) {
                            fadeFutureRef.get().cancel(false);
                            scheduler.shutdown();
                        }
                    }, 0, 30, TimeUnit.MILLISECONDS));

                    new ProgressBarAnimator(launcher, progressBar, progressText).startProgressTest();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, "fadeIn");
        });
    }

    /**
     * Метод выполняет анимацию исчезновения (fadeOut) экрана загрузки.
     * Для периодических вычислений создаётся локальный ScheduledExecutorService.
     */
    @Override
    public void fadeOut() {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            progressBar.setVisible(false);
            SwingUtilities.invokeLater(() -> {
                JLayeredPane layeredPane = launcher.getFrame().getLayeredPane();
                JPanel mainFramePanel = loggedForm.getPanel();
                for (Component component : layeredPane.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
                    if (component instanceof JPanel overlay && "loadingOverlay".equals(overlay.getName())) {
                        int initialAlpha = overlay.getBackground().getAlpha();
                        final long duration = 500;
                        final int interval = 20;
                        final long startTime = System.currentTimeMillis();
                        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                        AtomicReference<ScheduledFuture<?>> fadeOutFutureRef = new AtomicReference<>();
                        fadeOutFutureRef.set(scheduler.scheduleAtFixedRate(() -> {
                            long elapsed = System.currentTimeMillis() - startTime;
                            double progress = Math.min(1.0, (double) elapsed / duration);
                            double easedProgress = 1 - Math.pow(1 - progress, 2);
                            int newAlpha = (int) (initialAlpha * (1 - easedProgress));
                            SwingUtilities.invokeLater(() -> {
                                overlay.setBackground(new Color(0, 0, 0, newAlpha));
                                overlay.repaint();
                            });
                            if (progress >= 1.0) {
                                fadeOutFutureRef.get().cancel(false);
                                scheduler.shutdown();
                                SwingUtilities.invokeLater(() -> {
                                    layeredPane.remove(overlay);
                                    layeredPane.revalidate();
                                    layeredPane.repaint();
                                });
                            }
                        }, 0, interval, TimeUnit.MILLISECONDS));
                    }
                }
                setVisible(false);
                mainFramePanel.revalidate();
                mainFramePanel.repaint();
            });
        }, "fadeOut");
    }
}
