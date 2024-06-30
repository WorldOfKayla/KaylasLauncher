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
import java.awt.image.BufferedImage;

public class NotificationPopup extends JDialog implements NotificationDisplay {

    private static final int WIDTH = 600;
    public static final int HEIGHT = 100;
    private static final int DISPLAY_DURATION = 25000; // Duration in milliseconds
    private static final int ANIMATION_DURATION = 500; // Animation duration in milliseconds
    private static final int TIMER_DELAY = 10; // Delay for timer in milliseconds
    private static final int INITIAL_X = Toolkit.getDefaultToolkit().getScreenSize().width;

    private final NotificationUI notificationUI;
    private Point initialClick;
    private int initialX;
    private int initialY;
    private boolean hasSwipedRight;

    public NotificationPopup() {
        this.notificationUI = new NotificationUI(this); // Pass the instance of NotificationPopup to NotificationUI
        initializeUI();
        setContentPane(new BackgroundPanel());

        // Add mouse listener for dragging
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                initialX = getLocation().x;
                initialY = getLocation().y;
                hasSwipedRight = false;
            }

            public void mouseReleased(MouseEvent e) {
                if (hasSwipedRight) {
                    if (getX() > initialX + WIDTH / 4) {
                        // Swipe right - close notification
                        startFadeOutAnimation();
                    } else {
                        // Swipe left after swiping right - cancel close (reset position)
                        setLocation(initialX, initialY);
                        setOpacity(1.0f);
                    }
                } else {
                    // No significant swipe - reset position and opacity
                    setLocation(initialX, initialY);
                    setOpacity(1.0f);
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                int thisX = initialX + e.getX() - initialClick.x;

                if (thisX > initialX) {
                    hasSwipedRight = true;
                } else if (thisX < initialX && !hasSwipedRight) {
                    // Prevent moving left if not swiped right first
                    thisX = initialX;
                }

                // Ensure the notification stays within the bounds of initialX
                if (thisX < initialX) {
                    thisX = initialX;
                }

                setLocation(thisX, initialY);

                // Adjust opacity based on drag distance
                float dragDistance = Math.abs(initialX - thisX);
                float maxDistance = WIDTH / 4.0f;
                float opacity = 1.0f - Math.min(dragDistance / maxDistance, 1.0f);
                setOpacity(opacity);
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
    public void display(String title, String description, BufferedImage image) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            notificationUI.setupContent(title, description, image);
            setOpacity(0.9f);
            startSlideInAnimation();
            startAutoCloseTimer();

            setVisible(true);
        });
    }

    void startAutoCloseTimer() {
        Timer timer = new Timer(DISPLAY_DURATION, e -> startFadeOutAnimation());
        timer.setRepeats(false);
        timer.start();
    }

    /*
    private void startFadeInAnimation() {
        Timer fadeInTimer = new Timer(TIMER_DELAY, new ActionListener() {
            float opacity = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += (float) TIMER_DELAY / ANIMATION_DURATION;
                if (opacity >= 1) {
                    opacity = 1;
                    ((Timer) e.getSource()).stop();
                }
                setOpacity(opacity);
            }
        });
        fadeInTimer.start();
    } */

    private void startSlideInAnimation() {
        Timer slideInTimer = new Timer(TIMER_DELAY, new ActionListener() {
            int currentX = INITIAL_X;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentX -= WIDTH / (ANIMATION_DURATION / TIMER_DELAY);
                if (currentX <= Toolkit.getDefaultToolkit().getScreenSize().width - WIDTH - 10) {
                    currentX = Toolkit.getDefaultToolkit().getScreenSize().width - WIDTH - 10;
                    ((Timer) e.getSource()).stop();
                }
                setLocation(currentX, getLocation().y);
            }
        });
        slideInTimer.start();
    }

    void startFadeOutAnimation() {
        Timer fadeOutTimer = new Timer(TIMER_DELAY, new ActionListener() {
            float opacity = 1;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= (float) TIMER_DELAY / ANIMATION_DURATION;
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

    private class BackgroundPanel extends JPanel {
        public BackgroundPanel() {
            setBackground(new Color(34, 34, 34, 230));
            setBorder(new BevelBorder(BevelBorder.RAISED, Color.GRAY, Color.DARK_GRAY));
        }
    }

    private String getPropertyPrefix() {
        return "Notify";
    }
}