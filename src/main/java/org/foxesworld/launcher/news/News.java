package org.foxesworld.launcher.news;

import org.foxesworld.engine.Engine;
import org.foxesworld.launcher.news.provider.NewsProvider;

import javax.swing.*;

public class News extends org.foxesworld.engine.news.News {
    private final Engine engine;
    private NewsProvider newsProvider;
    public News(Engine engine){
        this.engine = engine;
        if(engine.getCONFIG().isLoadNews()) {
            this.newsProvider = new NewsProvider(engine);
            buildPanel();
        }
    }

    @Override
    protected void buildPanel(){
        JPanel childPanel = new NewsPanel(this.newsProvider.fetchNews());
        childPanel.setOpaque(false);
        childPanel.setBounds(0, 30, 500, 470);
        childPanel.setName("newsFrame");
        this.engine.getGuiBuilder().addPanelToMap(childPanel);
    }
}
