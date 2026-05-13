package com.jpomodoro.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MacNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(MacNotifier.class);
    private static final String SOUND_FOCUS_END = "/System/Library/Sounds/Glass.aiff";
    private static final String SOUND_BREAK_END = "/System/Library/Sounds/Hero.aiff";

    @Override
    public void notifyFocusEnded() {
        toast("Pomodoro", "Pause méritée — éloigne-toi de l'écran.");
        play(SOUND_FOCUS_END);
    }

    @Override
    public void notifyBreakEnded() {
        toast("Pomodoro", "Retour au focus, c'est reparti.");
        play(SOUND_BREAK_END);
    }

    @Override
    public void notifyCustom(String title, String body) {
        toast(title, body);
    }

    private void toast(String title, String message) {
        String safeMsg = message.replace("\"", "\\\"");
        String safeTitle = title.replace("\"", "\\\"");
        String script = "display notification \"" + safeMsg + "\" with title \"" + safeTitle + "\"";
        try {
            new ProcessBuilder("osascript", "-e", script).start();
        } catch (Exception e) {
            log.warn("Toast macOS impossible : {}", e.toString());
        }
    }

    private void play(String path) {
        try {
            new ProcessBuilder("afplay", path).start();
        } catch (Exception e) {
            log.warn("afplay impossible pour {} : {}", path, e.toString());
        }
    }
}
