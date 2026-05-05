package com.jpomodoro.notify;

public final class Notifier {

    private static final String SOUND_FOCUS_END = "/System/Library/Sounds/Glass.aiff";
    private static final String SOUND_BREAK_END = "/System/Library/Sounds/Hero.aiff";

    public static void notifyFocusEnded() {
        notify("Pomodoro", "Pause méritée — éloigne-toi de l'écran.");
        playSound(SOUND_FOCUS_END);
    }

    public static void notifyBreakEnded() {
        notify("Pomodoro", "Retour au focus, c'est reparti.");
        playSound(SOUND_BREAK_END);
    }

    private static void notify(String title, String message) {
        String safeMsg = message.replace("\"", "\\\"");
        String safeTitle = title.replace("\"", "\\\"");
        String script = "display notification \"" + safeMsg + "\" with title \"" + safeTitle + "\"";
        try {
            new ProcessBuilder("osascript", "-e", script).start();
        } catch (Exception ignored) {
        }
    }

    private static void playSound(String path) {
        try {
            new ProcessBuilder("afplay", path).start();
        } catch (Exception ignored) {
        }
    }

    private Notifier() {}
}
