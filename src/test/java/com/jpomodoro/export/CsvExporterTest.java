package com.jpomodoro.export;

import com.jpomodoro.db.SessionView;
import com.jpomodoro.timer.SessionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    @Test
    void escapesValuesWithSpecialChars() {
        assertThat(CsvExporter.escape("ok")).isEqualTo("ok");
        assertThat(CsvExporter.escape("with,comma")).isEqualTo("\"with,comma\"");
        assertThat(CsvExporter.escape("has \"quote\"")).isEqualTo("\"has \"\"quote\"\"\"");
        assertThat(CsvExporter.escape("multi\nline")).isEqualTo("\"multi\nline\"");
        assertThat(CsvExporter.escape(null)).isEmpty();
    }

    @Test
    void exportProducesHeaderAndRows(@TempDir Path tmp) throws Exception {
        Path target = tmp.resolve("export.csv");
        SessionView s = new SessionView(
                1L, SessionType.FOCUS,
                Instant.parse("2026-05-13T10:00:00Z"),
                Instant.parse("2026-05-13T10:25:00Z"),
                true, 7L, "fix bug", "résumé court", Boolean.TRUE);

        CsvExporter.export(target, List.of(s));

        String content = Files.readString(target);
        assertThat(content.lines().findFirst().orElseThrow()).startsWith("id,type,started_at");
        assertThat(content).contains("FOCUS").contains("1500").contains("fix bug").contains("positive");
    }

    @Test
    void exportEmptyOnlyHeader(@TempDir Path tmp) throws Exception {
        Path target = tmp.resolve("e.csv");
        CsvExporter.export(target, List.of());
        List<String> lines = Files.readAllLines(target);
        assertThat(lines).hasSize(1);
    }
}
