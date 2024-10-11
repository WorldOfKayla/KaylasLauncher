package org.foxesworld.Notification;

import com.formdev.flatlaf.ui.FlatUIUtils;
import org.foxesworld.notification.util.UIUtils;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationPopup extends JDialog implements NotificationDisplay {

    private static final int WIDTH = 600;
    public static final int HEIGHT = 100;
    private static final int DISPLAY_DURATION = 25000; // Duration in milliseconds
    private static final int ANIMATION_DURATION = 400; // Animation duration in milliseconds
    private static final int TIMER_DELAY = 10; // Delay for timer in milliseconds
    private static final int INITIAL_X = Toolkit.getDefaultToolkit().getScreenSize().width;

    private final NotificationUI notificationUI;
    private Timer autoCloseTimer;

    private static final ExecutorService notificationExecutor = Executors.newCachedThreadPool();

    public NotificationPopup() {
        this.notificationUI = new NotificationUI(this);
        initializeUI();
        setContentPane(new BackgroundPanel());
        setAlwaysOnTop(true);
        setFocusable(false);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                resetAutoCloseTimer();
            }
        });
    }

    private void initializeUI() {
        String prefix = getPropertyPrefix();
        Color closeIconColor = FlatUIUtils.getUIColor(prefix + ".closeIconColor", new Color(150, 150, 150));
        Icon closeButtonIcon = UIUtils.getIcon(prefix + ".closeIcon", UIUtils.createIcon("notification/close.svg", closeIconColor, 0.75F));
        notificationUI.setCloseButtonIcon(closeButtonIcon);
        setUndecorated(true);
        setSize(WIDTH, HEIGHT);
        setLocationOnScreen();
    }

    private void setLocationOnScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int taskBarSize = screenInsets.bottom;
        setLocation(screenSize.width - getWidth() - 10, screenSize.height - taskBarSize - getHeight() - 10);
    }

    @Override
    public void display(String title, String description, ImageIcon image) {
        notificationExecutor.execute(() -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                notificationUI.setupContent(title, description, image);
                setOpacity(0.0f);
                startSlideInAnimation();
                startAutoCloseTimer();
                setVisible(true);
            });
        });
    }

    private void startAutoCloseTimer() {
        autoCloseTimer = new Timer(DISPLAY_DURATION, e -> startFadeOutAnimation());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    private void resetAutoCloseTimer() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
            startAutoCloseTimer();
        }
    }

    void startFadeOutAnimation() {
        Timer fadeOutTimer = new Timer(TIMER_DELAY, new ActionListener() {
            float opacity = 1.0f;
            final float fadeSpeed = 1.0f / ANIMATION_DURATION;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= fadeSpeed * TIMER_DELAY;
                if (opacity <= 0) {
                    opacity = 0;
                    ((Timer) e.getSource()).stop();
                    dispose();
                }
                setOpacity(opacity);
            }
        });
        fadeOutTimer.start();
    }

    private void startSlideInAnimation() {
        Timer slideInTimer = new Timer(TIMER_DELAY, new ActionListener() {
            int currentX = INITIAL_X;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentX -= (WIDTH / 2) / (ANIMATION_DURATION / TIMER_DELAY / 1.5);
                setLocation(currentX, getLocation().y);

                if (currentX <= Toolkit.getDefaultToolkit().getScreenSize().width - WIDTH - 10) {
                    currentX = Toolkit.getDefaultToolkit().getScreenSize().width - WIDTH - 10;
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        slideInTimer.start();
    }

    private static class BackgroundPanel extends JPanel {
        public BackgroundPanel() {
            setBackground(new Color(255, 228, 196, 230)); // Light color for a friendly vibe
            setBorder(new BevelBorder(BevelBorder.RAISED, Color.LIGHT_GRAY, Color.GRAY));
        }
    }

    private String getPropertyPrefix() {
        return "Notify";
    }
}
