package com.jpomodoro.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jpomodoro.model.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class GoogleChatWebhook {

    private static final Logger log = LoggerFactory.getLogger(GoogleChatWebhook.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private GoogleChatWebhook() {}

    public static TestResult test(String url) {
        return post(url, "JPomodoro · message de test", Duration.ofSeconds(5));
    }

    public static void sendSummary(String url, Summary summary) {
        if (url == null || url.isBlank()) return;
        String text = "🍅 *Focus terminé*\n\n" + summary.content();
        TestResult res = post(url, text, Duration.ofSeconds(5));
        if (!res.success()) log.warn("Webhook envoi échec : {}", res.message());
    }

    public static TestResult post(String url, String text, Duration timeout) {
        if (url == null || url.isBlank()) return new TestResult(false, "URL vide");
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("text", text);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return new TestResult(true, "HTTP " + resp.statusCode());
            }
            return new TestResult(false, "HTTP " + resp.statusCode());
        } catch (Exception e) {
            return new TestResult(false, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public record TestResult(boolean success, String message) {}
}
