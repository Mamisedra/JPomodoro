package com.jpomodoro.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface Migration {
    int version();
    String description();
    void up(Connection conn) throws SQLException;
}
