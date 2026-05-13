package com.jpomodoro.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonlParserTest {

    @Test
    void parsesUserAndAssistantMessagesInWindow(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("session.jsonl");
        Files.writeString(file, """
                {"type":"user","timestamp":"2026-05-13T10:00:00Z","message":{"content":"hello"}}
                {"type":"assistant","timestamp":"2026-05-13T10:01:00Z","message":{"content":[{"type":"text","text":"hi there"}]}}
                {"type":"assistant","timestamp":"2026-05-13T10:30:00Z","message":{"content":"too late"}}
                {"type":"system","timestamp":"2026-05-13T10:02:00Z","message":{"content":"ignored"}}
                {"type":"user","timestamp":"2026-05-13T09:00:00Z","message":{"content":"too early"}}
                """);

        Instant from = Instant.parse("2026-05-13T09:30:00Z");
        Instant to = Instant.parse("2026-05-13T10:15:00Z");

        List<ParsedMessage> messages = JsonlParser.parseWindow(tmp, from, to);

        assertThat(messages).extracting(ParsedMessage::content).containsExactlyInAnyOrder("hello", "hi there");
        assertThat(messages).allSatisfy(m -> {
            assertThat(m.role()).isIn("user", "assistant");
            assertThat(m.timestamp()).isBetween(from, to);
        });
    }

    @Test
    void scansRecursively(@TempDir Path tmp) throws Exception {
        Path nested = Files.createDirectories(tmp.resolve("project-x/session-1"));
        Files.writeString(nested.resolve("a.jsonl"),
                "{\"type\":\"user\",\"timestamp\":\"2026-05-13T10:00:00Z\",\"message\":{\"content\":\"x\"}}\n");

        List<ParsedMessage> messages = JsonlParser.parseWindow(tmp,
                Instant.parse("2026-05-13T09:00:00Z"),
                Instant.parse("2026-05-13T11:00:00Z"));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).content()).isEqualTo("x");
    }

    @Test
    void invalidLinesAreSkipped(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("session.jsonl");
        Files.writeString(file, """
                not json at all
                {"type":"user","timestamp":"BAD_DATE","message":{"content":"ignored"}}
                {"type":"user","timestamp":"2026-05-13T10:00:00Z","message":{"content":"ok"}}
                """);

        List<ParsedMessage> messages = JsonlParser.parseWindow(tmp,
                Instant.parse("2026-05-13T09:00:00Z"),
                Instant.parse("2026-05-13T11:00:00Z"));

        assertThat(messages).extracting(ParsedMessage::content).containsExactly("ok");
    }

    @Test
    void missingBaseDirReturnsEmpty(@TempDir Path tmp) {
        Path absent = tmp.resolve("does-not-exist");
        List<ParsedMessage> messages = JsonlParser.parseWindow(absent,
                Instant.now().minusSeconds(60), Instant.now());
        assertThat(messages).isEmpty();
    }
}
