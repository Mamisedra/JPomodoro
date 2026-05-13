package com.jpomodoro.webhook;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleChatWebhookTest {

    @Test
    void emptyUrlReturnsFailureFast() {
        GoogleChatWebhook.TestResult r = GoogleChatWebhook.test("");
        assertThat(r.success()).isFalse();
        assertThat(r.message()).contains("vide");
    }

    @Test
    void invalidUrlReturnsFailure() {
        GoogleChatWebhook.TestResult r = GoogleChatWebhook.post(
                "http://127.0.0.1:1/bad-port-no-server", "hi", Duration.ofSeconds(1));
        assertThat(r.success()).isFalse();
    }
}
