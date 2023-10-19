package org.foxesworld.engine.sound;


import de.jarnbjo.vorbis.VorbisAudioFileReader;
import org.foxesworld.engine.AppFrame;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;

public class Sound {

    private AppFrame appFrame;
    private String baseDir = "assets/sounds/";
    private VorbisAudioFileReader vorbisAudioFileReader;

    public Sound(AppFrame appFrame) {
        this.appFrame = appFrame;
        vorbisAudioFileReader = new VorbisAudioFileReader();

    }

    public void playSound(String path){
        if((boolean) appFrame.getCONFIG().get("enableSound") == true) {
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
