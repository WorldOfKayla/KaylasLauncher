package org.takesome.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import org.takesome.kaylasEngine.gui.components.image.GifPlayer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;

public class GifPlayerFrame extends JFrame implements ActionListener {
    private final JButton openButton;
    private final JPanel contentPanel;
    private GifPlayer player;

    public GifPlayerFrame() {
        super("GIF Player");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        openButton = new JButton("Открыть GIF...");
        openButton.addActionListener(this);
        add(openButton, BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        setSize(600, 400);
        setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            if (player != null) {
                player.close();
            }
            contentPanel.removeAll();
            player = new GifPlayer(file);
            contentPanel.add(player, BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
            pack();
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки GIF: " + error.getMessage());
        }
    }

    @Override
    public void dispose() {
        if (player != null) {
            player.close();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        setupTheme("assets/theme/calista.properties");
        SwingUtilities.invokeLater(() -> {
            GifPlayerFrame frame = new GifPlayerFrame();
            frame.setVisible(true);
        });
    }

    public static void setupTheme(String theme) {
        try (InputStream themeStream = GifPlayerFrame.class.getClassLoader().getResourceAsStream(theme)) {
            if (themeStream == null) {
                throw new IllegalStateException("Theme file not found in resources: " + theme);
            }
            FlatPropertiesLaf laf = new FlatPropertiesLaf("Dark Theme", themeStream);
            FlatLaf.setup(laf);
        } catch (Exception error) {
            FlatLaf.setup(new FlatDarkLaf());
            error.printStackTrace();
        }
    }
}
