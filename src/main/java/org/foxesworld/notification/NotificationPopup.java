package org.foxesworld.notification;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class NotificationPopup extends JDialog implements NotificationDisplay {

    private static final int WIDTH = 600;
    public static final int HEIGHT = 100;
    private static final int DISPLAY_DURATION = 25000; // ms
    private static final int ANIMATION_DURATION = 400; // ms
    private static final int TIMER_DELAY = 10; // ms

    private final NotificationUI notificationUI;
    private Timer autoCloseTimer;

    public NotificationPopup() {
        this.notificationUI = new NotificationUI(this);
        initializeUI();
        setupListeners();
    }

    private void initializeUI() {
        setContentPane(new BackgroundPanel());
        setAlwaysOnTop(true);
        setUndecorated(true);
        setSize(WIDTH, HEIGHT);
        setLocationOnScreen();
    }

    private void setupListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startFadeOutAnimation();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                resetAutoCloseTimer();
            }
        });
    }

    private void setLocationOnScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int taskBarSize = screenInsets.bottom;
        setLocation(screenSize.width - WIDTH - 10, screenSize.height - taskBarSize - HEIGHT - 10);
    }

    @Override
    public void display(String title, String description, ImageIcon image) {
        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            notificationUI.setupContent(title, description, image);
            setOpacity(0.0f);
            startSlideInAnimation();
            startFadeInAnimation();
            startAutoCloseTimer();
            setVisible(true);
        });
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startFadeInAnimation() {
        animateOpacity(0.0f, 1.0f, ANIMATION_DURATION, true);
    }

    private void startAutoCloseTimer() {
        autoCloseTimer = new Timer(DISPLAY_DURATION, e -> startFadeOutAnimation());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    private void resetAutoCloseTimer() {
        if (autoCloseTimer != null) {
            autoCloseTimer.restart();
        }
    }

    void startFadeOutAnimation() {
        animateOpacity(1.0f, 0.0f, ANIMATION_DURATION, false);
    }

    private void animateOpacity(float startOpacity, float endOpacity, int duration, boolean fadeIn) {
        Timer timer = new Timer(TIMER_DELAY, new ActionListener() {
            final float fadeSpeed = (endOpacity - startOpacity) / duration * TIMER_DELAY;
            float opacity = startOpacity;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += fadeSpeed;
                if ((fadeIn && opacity >= endOpacity) || (!fadeIn && opacity <= endOpacity)) {
                    opacity = endOpacity;
                    ((Timer) e.getSource()).stop();
                    if (!fadeIn) dispose();
                }
                setOpacity(opacity);
            }
        });
        timer.start();
    }

    private void startSlideInAnimation() {
        Timer slideInTimer = new Timer(TIMER_DELAY, new ActionListener() {
            int currentX = Toolkit.getDefaultToolkit().getScreenSize().width;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentX -= (WIDTH / (ANIMATION_DURATION / TIMER_DELAY));
                setLocation(Math.max(currentX, Toolkit.getDefaultToolkit().getScreenSize().width - WIDTH - 10), getY());

                if (currentX <= Toolkit.getDefaultToolkit().getScreenSize().width - WIDTH - 10) {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        slideInTimer.start();
    }

    private static class BackgroundPanel extends JPanel {
        public BackgroundPanel() {
            setBackground(new Color(34, 34, 34, 230));
            setBorder(new LineBorder(Color.BLACK));
        }
    }

    private String getPropertyPrefix() {
        return "Notify";
    }
}
