package com.jpomodoro.notify;

public final class NotifierFactory {

    private NotifierFactory() {}

    public static Notifier current() {
        return forOs(System.getProperty("os.name", ""));
    }

    public static Notifier forOs(String osName) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) return new MacNotifier();
        if (os.contains("nux") || os.contains("nix")) return new LinuxNotifier();
        return new NoopNotifier();
    }
}
