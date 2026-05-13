package com.jpomodoro.notify;

public final class NotifierFactory {

    private NotifierFactory() {}

    public static Notifier current(SoundPlayer player) {
        return forOs(System.getProperty("os.name", ""), player);
    }

    public static Notifier forOs(String osName, SoundPlayer player) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) return new MacNotifier(player);
        if (os.contains("nux") || os.contains("nix")) return new LinuxNotifier(player);
        return new NoopNotifier();
    }
}
