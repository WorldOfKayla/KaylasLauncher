package org.foxesworld.launcher.news;

import org.foxesworld.Launcher;
import org.foxesworld.launcher.news.provider.NewsProvider;

import javax.swing.*;

public class News extends org.foxesworld.engine.news.News {
    private final Launcher launcher;
    private NewsProvider newsProvider;
    public News(Launcher launcher){
        this.launcher = launcher;
        if (this.launcher.getConfig().isLoadNews()) {
            this.newsProvider = new NewsProvider(this.launcher);
        }
        buildPanel();
    }

    @Override
    protected void buildPanel(){
        JPanel childPanel = new NewsPanel(this);
        childPanel.setOpaque(false);
        childPanel.setBounds(0, 0, 505, 480);
        childPanel.setName("newsFrame");
        this.launcher.getGuiBuilder().addPanelToMap(childPanel);
    }

    public NewsProvider getNewsProvider() {
        return newsProvider;
    }

    public Launcher getLauncher() {
        return launcher;
    }
}
