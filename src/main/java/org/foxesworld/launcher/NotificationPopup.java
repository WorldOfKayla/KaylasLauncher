package org.foxesworld.launcher;

import com.formdev.flatlaf.ui.FlatUIUtils;
import org.foxesworld.notification.util.UIUtils;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

interface NotificationDisplay {
    void display(String title, String description, BufferedImage image);
}

public class NotificationPopup extends JDialog implements NotificationDisplay {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 100;
    private static final int DISPLAY_DURATION = 25000; // Duration in milliseconds
    private static final int ANIMATION_DURATION = 500; // Animation duration in milliseconds
    private static final int TIMER_DELAY = 10; // Delay for timer in milliseconds
    private static final Color BORDER_COLOR = Color.BLACK;

    private Icon closeButtonIcon;
    private Color closeIconColor;
    private Timer fadeInTimer;
    private Timer fadeOutTimer;

    public NotificationPopup() {
        initializeUI();
        setContentPane(new BackgroundPanel());
    }

    private void initializeUI() {
        String prefix = getPropertyPrefix();
        closeIconColor = FlatUIUtils.getUIColor(prefix + ".closeIconColor", new Color(150, 150, 150));
        closeButtonIcon = UIUtils.getIcon(prefix + ".closeIcon", UIUtils.createIcon("notification/close.svg", closeIconColor, 0.75F));
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

    private void setupContent(String title, String description, BufferedImage image) {
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());

        GridBagConstraints constraints = createGridBagConstraints();

        // Image resizing
        Image scaledImage = image.getScaledInstance(HEIGHT - 25, HEIGHT - 25, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImage);
        JLabel imageLabel = new JLabel(icon);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.weightx = 0f;
        constraints.insets = new Insets(10, 10, 10, 10);
        constraints.anchor = GridBagConstraints.WEST;
        contentPane.add(imageLabel, constraints);

        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(113, 198, 71));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 1.0f;
        constraints.insets = new Insets(20, 10, 0, 10);
        contentPane.add(titleLabel, constraints);

        // Description label
        JLabel descriptionLabel = new JLabel("<html>" + description + "</html>");
        descriptionLabel.setForeground(Color.WHITE);
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 10, 35, 10);
        contentPane.add(descriptionLabel, constraints);

        // Close button
        JButton closeButton = createCloseButton();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.insets = new Insets(5, 0, 0, 5);
        contentPane.add(closeButton, constraints);
    }

    protected String getPropertyPrefix() {
        return "Notify";
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weighty = 1.0f;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.BOTH;
        return constraints;
    }

    protected JButton createCloseButton() {
        JButton button = new JButton(null, closeButtonIcon);
        button.setPreferredSize(new Dimension(16, 16));
        button.setFocusable(false);
        button.addActionListener((e) -> startFadeOutAnimation());
        return button;
    }

    private void startAutoCloseTimer() {
        Timer timer = new Timer(DISPLAY_DURATION, e -> startFadeOutAnimation());
        timer.setRepeats(false);
        timer.start();
    }

    private void startFadeInAnimation() {
        fadeInTimer = new Timer(TIMER_DELAY, new ActionListener() {
            float opacity = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += (float) TIMER_DELAY / ANIMATION_DURATION;
                if (opacity >= 1) {
                    opacity = 1;
                    fadeInTimer.stop();
                }
                setOpacity(opacity);
            }
        });
        fadeInTimer.start();
    }

    private void startFadeOutAnimation() {
        fadeOutTimer = new Timer(TIMER_DELAY, new ActionListener() {
            float opacity = 1;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= (float) TIMER_DELAY / ANIMATION_DURATION;
                if (opacity <= 0) {
                    opacity = 0;
                    fadeOutTimer.stop();
                    dispose();
                }
                setOpacity(opacity);
            }
        });
        fadeOutTimer.start();
    }

    @Override
    public void display(String title, String description, BufferedImage image) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            setupContent(title, description, image);
            setOpacity(0.9f);
            startFadeInAnimation();
            startAutoCloseTimer();

            setVisible(true);
        });
    }

    private class BackgroundPanel extends JPanel {
        public BackgroundPanel() {
            setBackground(new Color(34, 34, 34, 230));
            setBorder(new BevelBorder(BevelBorder.RAISED, Color.GRAY, Color.DARK_GRAY));
        }
    }
}
