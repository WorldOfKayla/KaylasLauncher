package org.foxesworld.engine.gui.components.button;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class Button extends JButton implements MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 1L;
	private boolean entered = false;
	private boolean pressed = false;
	public BufferedImage defaultTX;
	public BufferedImage rolloverTX;
	public BufferedImage pressedTX;
	public BufferedImage lockedTX;

	public Button(String text) {
		addMouseListener(this);
		addMouseMotionListener(this);
		setText(text);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setFocusPainted(false);
		setOpaque(false);
		setFocusable(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public Button(ImageIcon icon) {
		super(icon);
		addMouseListener(this);
		addMouseMotionListener(this);
		setText("");
		setBorderPainted(false);
		setContentAreaFilled(false);
		setFocusPainted(false);
		setOpaque(false);
		setFocusable(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g.create();
		//g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.KEY_ANTIALIASING);

		int w = getWidth();
		int h = getHeight();

		BufferedImage imageToDraw = defaultTX;

		if (!isEnabled()) {
			imageToDraw = lockedTX;
		} else if (pressed) {
			imageToDraw = pressedTX;
		} else if (entered) {
			imageToDraw = rolloverTX;
		}

		super.paintComponent(g);
		g2d.drawImage(imageToDraw, 0, 0, w, h, null);

		// DrawText
		if (getText() != null && !getText().isEmpty()) {
			FontMetrics fm = g.getFontMetrics();
			int textX = (w - fm.stringWidth(getText())) / 2;
			int textY = (h + fm.getAscent()) / 2;
			g.setColor(getForeground());
			g.drawString(getText(), textX, textY);
		}

		g2d.dispose();
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		entered = true;
		repaint();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		entered = false;
		repaint();
	}
	@Override
	public void mousePressed(MouseEvent e) {
		if (isEnabled() && e.getButton() == MouseEvent.BUTTON1) {
			pressed = true;
			repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (pressed && e.getButton() == MouseEvent.BUTTON1) {
			pressed = false;
			repaint();
		}
	}
	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {}
}
