package org.foxesworld.newengine;

import org.foxesworld.newengine.locale.LocalizationLoader;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;


public class FormCreatorApp {
    private static FormCreatorApp APP;
    private static LocalizationLoader LANG;
    private String LOCALE = "ru";
    private InputStream langFile = FormCreatorApp.class.getClassLoader().getResourceAsStream("locale.json");

    public static void main(String[] args) {
        APP = new FormCreatorApp();
        LANG = new LocalizationLoader(APP);
        JFrame frame = new JFrame(LANG.getString("appTitle"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(0, 2));

        JLabel nameLabel = new JLabel(LANG.getString("nameLabel"));
        JTextField nameField = new JTextField();

        JLabel ageLabel = new JLabel(LANG.getString("ageLabel"));
        JTextField ageField = new JTextField();

        formPanel.add(nameLabel);
        formPanel.add(nameField);
        formPanel.add(ageLabel);
        formPanel.add(ageField);
        frame.add(formPanel, BorderLayout.CENTER);
        JButton submitButton = new JButton(LANG.getString("submit"));
        frame.add(submitButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    public String getLOCALE() {
        return LOCALE;
    }

    public InputStream getLangFile() {
        return langFile;
    }
}
