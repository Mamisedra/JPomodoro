package com.jpomodoro.ui;

public enum Tab {
    TIMER("Timer"),
    HISTORY("Historique"),
    SETTINGS("Réglages");

    private final String label;

    Tab(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public Tab next() {
        Tab[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
