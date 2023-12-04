package org.foxesworld.engine.news;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.news.provider.NewsProvider;

import javax.swing.*;

public class News {
    private final Engine engine;
    private NewsProvider newsProvider;
    public News(Engine engine){
        this.engine = engine;
        if(engine.getCONFIG().isLoadNews()) {
            this.newsProvider = new NewsProvider(engine);
            buildPanel();
        }
    }

    private void buildPanel(){
        JPanel childPanel = new NewsPanel(this.newsProvider.fetchNews());
        childPanel.setOpaque(false);
        childPanel.setBounds(0, 30, 500, 470);
        childPanel.setName("newsFrame");
        this.engine.getGuiBuilder().addPanelToMap(childPanel);
    }
}
