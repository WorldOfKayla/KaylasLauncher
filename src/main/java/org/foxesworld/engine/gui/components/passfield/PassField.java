package org.foxesworld.engine.gui.components.passfield;

import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class PassField extends JPasswordField {
    BufferedImage texture;
    private String placeholder;
    private boolean caretVisible = true;
    private int paddingX;
    private int paddingY;
    private Timer caretTimer;

    public PassField(String placeholder) {
        this.placeholder = placeholder;
        this.setOpaque(false);

        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
                startCaretBlinking();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
                stopCaretBlinking();
            }
        });
    }

    private void startCaretBlinking() {
        if (caretTimer == null || !caretTimer.isRunning()) {
            caretTimer = new Timer(500, new ActionListener() {
                private boolean caretVisibleState = true;

                @Override
                public void actionPerformed(ActionEvent e) {
                    caretVisible = caretVisibleState;
                    caretVisibleState = !caretVisibleState;
                    repaint();
                }
            });
            caretTimer.start();
        }
    }

    private void stopCaretBlinking() {
        if (caretTimer != null) {
            caretTimer.stop();
        }
        caretVisible = true;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(ImageUtils.genButton(getWidth(), getHeight(), texture), 0, 0, getWidth(), getHeight(), null);

        if (!hasFocus() && getPassword().length == 0 && placeholder != null) {
            g2.drawString(placeholder, getInsets().left + paddingX, g.getFontMetrics().getMaxAscent() + getInsets().top + paddingY);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the password characters
            char[] password = getPassword();
            String maskedPassword = new String(password).replaceAll(".", "*");
            int x = getInsets().left + paddingX;
            int y = g.getFontMetrics().getMaxAscent() + getInsets().top + paddingY;

            g2.drawString(maskedPassword, x, y);

            // Draw the caret only when visible and the password field has focus
            if (isFocusOwner() && caretVisible) {
                int caretX = x + g.getFontMetrics().stringWidth(maskedPassword.substring(0, getCaretPosition()));
                g2.drawLine(caretX, y - g.getFontMetrics().getAscent(), caretX, y + g.getFontMetrics().getDescent());
            }
        }
    }

    public void setPaddingX(int paddingX) {
        this.paddingX = paddingX;
    }

    public void setPaddingY(int paddingY) {
        this.paddingY = paddingY;
    }
}
