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
    private JProgressBar progressBar;
    private Label progressText;


    public LoadStatus(Launcher launcher, int index) {
        super(launcher);
        this.launcher = launcher;

        this.attributesList = List.of(this.engine.getEngineData().getLoadManager());
        this.loadingText = engine.getLANG().getString("loading.msg");
        this.loadingTitle = engine.getLANG().getString("loading.title");

        // Установка скорости анимации согласно параметрам конфигурации
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
                progressBar = (JProgressBar) loadPanel.getComponent("loadProgress");
                progressText = (Label) loadPanel.getComponent("progressText");
                progressBar.add(progressText);
                loaderText.setForeground(hexToColor(attributes.getDescColor()));
                titleLabel.setForeground(hexToColor(attributes.getTitleColor()));
            } catch (Exception ex) {
                ex.printStackTrace();
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
     * Он выполняет отключение основных компонентов и накладывает затемняющий оверлей.
     */
    @Override
    public void fadeIn() {
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

                    final AtomicInteger alpha = new AtomicInteger(0);
                    Timer fadeTimer = new Timer(30, e -> {
                        int newAlpha = Math.min(alpha.addAndGet(10), 180);
                        overlay.setBackground(new Color(0, 0, 0, newAlpha));
                        overlay.repaint();
                        if (newAlpha >= 180) {
                            ((Timer) e.getSource()).stop();
                        }
                    });
                    fadeTimer.start();
                });

                new ProgressBarAnimator(launcher, progressBar, progressText).startProgressTest();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "fadeIn");
    }



    /**
     * Метод выполняет анимацию исчезновения (fadeOut) экрана загрузки.
     * После завершения анимации происходит удаление оверлея и обновление основных компонентов.
     */
    @Override
    public void fadeOut() {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            this.progressBar.setVisible(false);
            try {
                SwingUtilities.invokeLater(() -> {
                    JLayeredPane layeredPane = launcher.getFrame().getLayeredPane();
                    JPanel mainFramePanel = loggedForm.getPanel();
                    for (Component component : layeredPane.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
                        if (component instanceof JPanel overlay && "loadingOverlay".equals(overlay.getName())) {
                            final int initialAlpha = overlay.getBackground().getAlpha();
                            // Общая длительность анимации (в миллисекундах)
                            final long duration = 500;
                            final int interval = 20;
                            // Время начала анимации
                            final long startTime = System.currentTimeMillis();

                            Timer timer = new Timer(interval, null);
                            timer.addActionListener(e -> {
                                // Вычисляем прошедшее время
                                long elapsed = System.currentTimeMillis() - startTime;
                                double progress = Math.min(1.0, (double) elapsed / duration);
                                // Применяем функцию ease-out: easedProgress = 1 - (1 - progress)^2
                                double easedProgress = 1 - Math.pow(1 - progress, 2);
                                // Вычисляем новое значение альфа с учётом ease-out эффекта
                                int newAlpha = (int) (initialAlpha * (1 - easedProgress));
                                overlay.setBackground(new Color(0, 0, 0, newAlpha));
                                overlay.repaint();

                                if (progress >= 1.0) {
                                    ((Timer) e.getSource()).stop();
                                    layeredPane.remove(overlay);
                                    layeredPane.revalidate();
                                    layeredPane.repaint();
                                }
                            });
                            timer.start();
                        }
                    }
                    // Скрываем текущее окно загрузки и обновляем основную панель
                    setVisible(false);
                    mainFramePanel.revalidate();
                    mainFramePanel.repaint();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "fadeOut");
    }

}
