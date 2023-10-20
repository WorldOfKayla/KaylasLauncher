package org.foxesworld.engine.sound;


import de.jarnbjo.vorbis.VorbisAudioFileReader;
import org.foxesworld.engine.Engine;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;

public class Sound {

    private Engine engine;
    private String baseDir = "assets/sounds/";
    private VorbisAudioFileReader vorbisAudioFileReader;

    public Sound(Engine engine) {
        this.engine = engine;
        vorbisAudioFileReader = new VorbisAudioFileReader();

    }

    public void playSound(String path){
        if((boolean) engine.getCONFIG().get("enableSound") == true) {
            String fullPath = baseDir + path;
            try {
                InputStream inputStream = Sound.class.getClassLoader().getResourceAsStream(fullPath);
                AudioInputStream audioInputStream = vorbisAudioFileReader.getAudioInputStream(inputStream);

                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);

                clip.start();
            } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
        }
    }
}
