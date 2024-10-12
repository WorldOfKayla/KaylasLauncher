package org.foxesworld.notification;

import com.formdev.flatlaf.extras.components.FlatButton;
import com.formdev.flatlaf.extras.components.FlatLabel;

import javax.swing.*;
import java.awt.*;

class NotificationUI {

    private final NotificationPopup notificationPopup;
    private Icon closeButtonIcon;

    public NotificationUI(NotificationPopup notificationPopup) {
        this.notificationPopup = notificationPopup;
    }

    public void setCloseButtonIcon(Icon closeButtonIcon) {
        this.closeButtonIcon = closeButtonIcon;
    }

    public void setupContent(String title, String description, ImageIcon image) {
        Container contentPane = notificationPopup.getContentPane(); // Get content pane of NotificationPopup

        // Remove all existing components
        contentPane.removeAll();

        // Set layout
        contentPane.setLayout(new GridBagLayout());

        GridBagConstraints constraints = createGridBagConstraints();

        // Image label
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(resizeImageIcon(image));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(10, 10, 10, 10);
        constraints.anchor = GridBagConstraints.WEST;
        contentPane.add(imageLabel, constraints);

        // Title label
        FlatLabel titleLabel = new FlatLabel();
        titleLabel.setText(title);
        titleLabel.setForeground(new Color(113, 198, 71));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(20, 10, 0, 10);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        contentPane.add(titleLabel, constraints);

        // Description label
        FlatLabel descriptionLabel = new FlatLabel();
        descriptionLabel.setText("<html>" + description + "</html>");
        descriptionLabel.setForeground(Color.WHITE);
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 10, 35, 10);
        contentPane.add(descriptionLabel, constraints);

        // Close button
        JButton closeButton = createCloseButton();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.insets = new Insets(5, 0, 0, 10);
        contentPane.add(closeButton, constraints);

        // Validate and repaint to ensure components are updated
        notificationPopup.validate();
        notificationPopup.repaint();
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(5, 5, 5, 5);
        return constraints;
    }

    private JButton createCloseButton() {
        FlatButton button = new FlatButton();
        button.setIcon(closeButtonIcon);
        button.setPreferredSize(new Dimension(16, 16));
        button.setFocusable(false);
        button.addActionListener((e) -> {
            notificationPopup.startFadeOutAnimation(); // Start fade out animation on close button click
        });
        return button;
    }

    private ImageIcon resizeImageIcon(ImageIcon icon) {
        int height = NotificationPopup.HEIGHT - 25;
        Image image = icon.getImage();
        Image resizedImage = image.getScaledInstance(height, height, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }
}