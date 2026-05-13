package com.jpomodoro.ai;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryPromptTemplateTest {

    @Test
    void includesAllMessages() {
        List<ParsedMessage> messages = List.of(
                new ParsedMessage("user", "fix bug", Instant.parse("2026-05-13T10:00:00Z")),
                new ParsedMessage("assistant", "diagnosed", Instant.parse("2026-05-13T10:01:00Z"))
        );
        String prompt = SummaryPromptTemplate.build(messages);

        assertThat(prompt).contains("fix bug").contains("diagnosed");
        assertThat(prompt).contains("Résumé :");
        assertThat(prompt).contains("user :").contains("assistant :");
    }

    @Test
    void handlesEmptyInput() {
        String prompt = SummaryPromptTemplate.build(List.of());
        assertThat(prompt).contains("(Aucun message capturé");
    }

    @Test
    void truncatesVeryLongContent() {
        String big = "x".repeat(1000);
        List<ParsedMessage> messages = List.of(
                new ParsedMessage("user", big, Instant.parse("2026-05-13T10:00:00Z")));
        String prompt = SummaryPromptTemplate.build(messages);
        assertThat(prompt).contains("…");
    }
}
