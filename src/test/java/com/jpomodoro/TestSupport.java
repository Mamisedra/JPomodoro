package com.jpomodoro;

import com.jpomodoro.config.AppPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public final class TestSupport {

    private TestSupport() {}

    public static AppPaths tempPaths() throws IOException {
        Path dir = Files.createTempDirectory("jpomodoro-test-");
        return new AppPaths(dir);
    }

    public static Connection inMemoryDb() {
        try {
            return DriverManager.getConnection("jdbc:sqlite::memory:");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
