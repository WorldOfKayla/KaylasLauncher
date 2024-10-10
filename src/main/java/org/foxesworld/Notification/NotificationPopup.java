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

public class NotificationPopup extends JDialog implements NotificationDisplay {

    private static final int WIDTH = 600;
    public static final int HEIGHT = 100;
    private static final int DISPLAY_DURATION = 25000; // Duration in milliseconds
    private static final int ANIMATION_DURATION = 400; // Animation duration in milliseconds
    private static final int TIMER_DELAY = 10; // Delay for timer in milliseconds
    private static final int INITIAL_X = Toolkit.getDefaultToolkit().getScreenSize().width;

    private final NotificationUI notificationUI;
    private Point initialClick;
    private int initialX;
    private int initialY;
    private boolean hasSwipedRight;
    private Timer autoCloseTimer;

    public NotificationPopup() {
        this.notificationUI = new NotificationUI(this); // Pass the instance of NotificationPopup to NotificationUI
        initializeUI();
        setContentPane(new BackgroundPanel());
        setAlwaysOnTop(true);

        // Add mouse listener for dragging
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                initialX = getLocation().x;
                initialY = getLocation().y;
                hasSwipedRight = false;
                startFadeOutAnimation();
            }

            public void mouseClicked(MouseEvent e) {
                resetAutoCloseTimer(); // Reset the timer on mouse click
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
        setLocationRelativeTo(null);
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
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            notificationUI.setupContent(title, description, image);
            setOpacity(0.0f); // Start with 0 opacity
            startSlideInAnimation();

            // Fade in animation
            Timer fadeInTimer = new Timer(TIMER_DELAY, new ActionListener() {
                float opacity = 0.0f;
                final float fadeSpeed = 1.0f / ANIMATION_DURATION; // Speed of opacity change

                @Override
                public void actionPerformed(ActionEvent e) {
                    opacity += fadeSpeed * TIMER_DELAY;
                    if (opacity >= 1.0f) {
                        opacity = 1.0f;
                        ((Timer) e.getSource()).stop();
                    }
                    setOpacity(opacity);
                }
            });
            fadeInTimer.start();

            startAutoCloseTimer();
            setVisible(true);
        });
    }

    void startAutoCloseTimer() {
        autoCloseTimer = new Timer(DISPLAY_DURATION, e -> startFadeOutAnimation());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    void resetAutoCloseTimer() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
            startAutoCloseTimer();
        }
    }

    void startFadeOutAnimation() {
        Timer fadeOutTimer = new Timer(TIMER_DELAY, new ActionListener() {
            float opacity = 1.0f;
            float fadeSpeed = 1.0f / ANIMATION_DURATION; // Speed of opacity change

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= fadeSpeed * TIMER_DELAY;
                if (opacity <= 0) {
                    opacity = 0;
                    ((Timer) e.getSource()).stop();
                    dispose();
                }
                setOpacity(opacity);
                fadeSpeed *= 1.1; // Accelerate the fade-out towards the end
            }
        });
        fadeOutTimer.start();
    }

    void startSlideInAnimation() {
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
            setBackground(new Color(34, 34, 34, 230));
            setBorder(new BevelBorder(BevelBorder.RAISED, Color.GRAY, Color.DARK_GRAY));
        }
    }

    private String getPropertyPrefix() {
        return "Notify";
    }
}
