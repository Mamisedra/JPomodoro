package com.jpomodoro.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinuxNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(LinuxNotifier.class);

    @Override
    public void notifyFocusEnded() {
        toast("Pomodoro", "Pause méritée — éloigne-toi de l'écran.");
        bell();
    }

    @Override
    public void notifyBreakEnded() {
        toast("Pomodoro", "Retour au focus, c'est reparti.");
        bell();
    }

    @Override
    public void notifyCustom(String title, String body) {
        toast(title, body);
    }

    private void toast(String title, String body) {
        try {
            new ProcessBuilder("notify-send", "--app-name=JPomodoro", title, body)
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {
            log.warn("notify-send indisponible : {}", e.toString());
        }
    }

    private void bell() {
        System.out.print('\007');
        System.out.flush();
    }
}
