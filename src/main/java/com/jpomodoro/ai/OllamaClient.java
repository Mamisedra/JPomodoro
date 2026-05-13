package com.jpomodoro.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public final class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String endpoint;
    private final Duration timeout;
    private final HttpClient httpClient;

    public OllamaClient(String endpoint, Duration timeout) {
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<String> generate(String model, String prompt) {
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/api/generate"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Ollama HTTP {} : {}", resp.statusCode(), trim(resp.body()));
                return Optional.empty();
            }
            return parseResponse(resp.body());
        } catch (Exception e) {
            log.warn("Ollama generate échec : {}", e.toString());
            return Optional.empty();
        }
    }

    public boolean ping() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    static Optional<String> parseResponse(String body) {
        try {
            JsonNode node = MAPPER.readTree(body);
            String response = node.path("response").asText(null);
            if (response == null || response.isBlank()) return Optional.empty();
            return Optional.of(response.trim());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
