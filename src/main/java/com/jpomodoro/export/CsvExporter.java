package com.jpomodoro.export;

import com.jpomodoro.db.SessionView;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvExporter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final String[] HEADERS = {
            "id", "type", "started_at", "ended_at", "duration_seconds",
            "completed", "task_title", "summary", "feedback"
    };

    private CsvExporter() {}

    public static Path defaultPath(Path baseDir) {
        return baseDir.resolve("export-" + LocalDateTime.now().format(STAMP) + ".csv");
    }

    public static Path export(Path target, List<SessionView> rows) throws IOException {
        Files.createDirectories(target.getParent());
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(target, StandardCharsets.UTF_8))) {
            w.println(String.join(",", HEADERS));
            for (SessionView s : rows) {
                w.println(String.join(",",
                        Long.toString(s.id()),
                        s.type().name(),
                        s.startedAt() == null ? "" : s.startedAt().atZone(ZoneId.systemDefault()).toLocalDateTime().toString(),
                        s.endedAt() == null ? "" : s.endedAt().atZone(ZoneId.systemDefault()).toLocalDateTime().toString(),
                        Long.toString(s.durationSeconds()),
                        s.completed() ? "1" : "0",
                        escape(s.taskTitle()),
                        escape(s.summary()),
                        s.feedback() == null ? "" : (s.feedback() ? "positive" : "negative")
                ));
            }
        }
        return target;
    }

    static String escape(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        boolean needsQuotes = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r");
        String body = raw.replace("\"", "\"\"");
        return needsQuotes ? "\"" + body + "\"" : body;
    }
}
