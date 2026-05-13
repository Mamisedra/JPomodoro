package com.jpomodoro.notify;

import com.jpomodoro.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Set;

public final class SoundPlayer {

    private static final Logger log = LoggerFactory.getLogger(SoundPlayer.class);
    private static final Set<String> PRESETS = Set.of("bell", "chime", "ding", "soft");
    static final String NONE = "none";

    private final ConfigService config;

    public SoundPlayer(ConfigService config) {
        this.config = config;
    }

    public void play() {
        String name = config.get().notification().sound();
        play(name);
    }

    public void play(String name) {
        if (name == null || name.isBlank() || NONE.equalsIgnoreCase(name)) return;
        String preset = PRESETS.contains(name.toLowerCase()) ? name.toLowerCase() : "bell";
        String resource = "/sounds/" + preset + ".wav";
        try (InputStream in = SoundPlayer.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("Son introuvable : {}", resource);
                return;
            }
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in))) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.addLineListener(e -> {
                    if (e.getType() == LineEvent.Type.STOP) clip.close();
                });
                clip.start();
            }
        } catch (Exception e) {
            log.warn("Lecture son {} échouée : {}", preset, e.toString());
        }
    }

    public static Set<String> presets() {
        return PRESETS;
    }
}
