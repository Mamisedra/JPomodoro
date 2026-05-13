package com.jpomodoro.db;

import com.jpomodoro.config.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public final class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static Connection connection;

    public static synchronized Connection get() {
        if (connection != null) return connection;
        Path dbPath = new AppPaths().database();
        return open(dbPath);
    }

    public static synchronized Connection open(Path dbPath) {
        if (connection != null) return connection;
        try {
            dbPath.getParent().toFile().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            log.info("DB ouverte : {}", dbPath);
            MigrationRunner.run(connection);
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'initialiser la base SQLite", e);
        }
    }

    static synchronized void resetForTests() {
        try {
            if (connection != null) connection.close();
        } catch (Exception ignored) {}
        connection = null;
    }

    private Database() {}
}
