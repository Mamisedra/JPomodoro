package com.jpomodoro.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaClientTest {

    @Test
    void parseResponseExtractsField() {
        String body = "{\"model\":\"qwen2.5:7b\",\"response\":\"hello world\",\"done\":true}";
        Optional<String> result = OllamaClient.parseResponse(body);
        assertThat(result).contains("hello world");
    }

    @Test
    void parseResponseTrimsWhitespace() {
        Optional<String> result = OllamaClient.parseResponse("{\"response\":\"   hi   \"}");
        assertThat(result).contains("hi");
    }

    @Test
    void parseResponseEmptyOnMissingField() {
        assertThat(OllamaClient.parseResponse("{\"done\":true}")).isEmpty();
        assertThat(OllamaClient.parseResponse("{\"response\":\"\"}")).isEmpty();
    }

    @Test
    void parseResponseEmptyOnInvalidJson() {
        assertThat(OllamaClient.parseResponse("not json")).isEmpty();
    }
}
