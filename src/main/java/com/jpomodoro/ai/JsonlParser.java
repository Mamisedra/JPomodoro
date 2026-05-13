package com.jpomodoro.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class JsonlParser {

    private static final Logger log = LoggerFactory.getLogger(JsonlParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonlParser() {}

    public static Path defaultClaudeProjectsDir() {
        return Path.of(System.getProperty("user.home"), ".claude", "projects");
    }

    /**
     * Lit tous les fichiers .jsonl sous {@code baseDir} et retourne les messages dont
     * le timestamp est dans [from, to]. Streaming line-by-line, filtrage rapide avant
     * parse complet pour réduire l'impact mémoire.
     */
    public static List<ParsedMessage> parseWindow(Path baseDir, Instant from, Instant to) {
        if (baseDir == null || !Files.isDirectory(baseDir)) return List.of();
        List<ParsedMessage> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(baseDir)) {
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".jsonl"))
                .forEach(p -> parseFile(p, from, to, out));
        } catch (IOException e) {
            log.warn("Scan JSONL échoué sous {} : {}", baseDir, e.toString());
        }
        return out;
    }

    private static void parseFile(Path file, Instant from, Instant to, List<ParsedMessage> out) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                ParsedMessage msg = parseLine(line, from, to);
                if (msg != null) out.add(msg);
            }
        } catch (IOException e) {
            log.debug("Lecture {} échouée : {}", file, e.toString());
        }
    }

    static ParsedMessage parseLine(String line, Instant from, Instant to) {
        try {
            JsonNode node = MAPPER.readTree(line);
            String tsRaw = textOrNull(node.path("timestamp"));
            if (tsRaw == null) return null;
            Instant ts;
            try {
                ts = Instant.parse(tsRaw);
            } catch (DateTimeParseException e) {
                return null;
            }
            if (ts.isBefore(from) || ts.isAfter(to)) return null;

            String role = textOrNull(node.path("type"));
            if (role == null) role = textOrNull(node.path("role"));
            if (role == null) return null;
            if (!role.equals("user") && !role.equals("assistant")) return null;

            String content = extractContent(node);
            if (content == null || content.isBlank()) return null;
            return new ParsedMessage(role, content.trim(), ts);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractContent(JsonNode node) {
        JsonNode msg = node.path("message");
        JsonNode content = msg.isMissingNode() ? node.path("content") : msg.path("content");
        if (content.isTextual()) return content.asText();
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                String text = textOrNull(item.path("text"));
                if (text != null) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(text);
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
