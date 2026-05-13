package com.jpomodoro.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {

    private final Path home;

    public AppPaths() {
        this(Path.of(System.getProperty("user.home"), ".pomodoro"));
    }

    public AppPaths(Path home) {
        this.home = home;
    }

    public Path home() { return home; }
    public Path database() { return home.resolve("data.db"); }
    public Path config() { return home.resolve("config.toml"); }
    public Path lock() { return home.resolve(".lock"); }
    public Path logs() { return home.resolve("logs"); }
    public Path logFile() { return logs().resolve("pomodoro.log"); }

    public void ensureDirectories() {
        try {
            Files.createDirectories(home);
            Files.createDirectories(logs());
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire " + home, e);
        }
    }
}
