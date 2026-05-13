package com.jpomodoro.ai;

import com.jpomodoro.config.AppConfig;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.db.SummaryRepository;
import com.jpomodoro.model.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final ConfigService config;
    private final SummaryRepository summaries;
    private final ExecutorService executor;
    private final Path jsonlBaseDir;

    public SummaryService(ConfigService config, SummaryRepository summaries,
                          ExecutorService executor, Path jsonlBaseDir) {
        this.config = config;
        this.summaries = summaries;
        this.executor = executor;
        this.jsonlBaseDir = jsonlBaseDir;
    }

    public CompletableFuture<Optional<Summary>> summarizeAsync(long sessionId, Instant from, Instant to) {
        if (!config.get().ai().enabled()) {
            log.debug("IA désactivée — résumé sauté");
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.supplyAsync(() -> runSummarize(sessionId, from, to), executor);
    }

    private Optional<Summary> runSummarize(long sessionId, Instant from, Instant to) {
        long t0 = System.currentTimeMillis();
        AppConfig.AiSettings ai = config.get().ai();

        List<ParsedMessage> messages = JsonlParser.parseWindow(jsonlBaseDir, from, to);
        log.info("Résumé session {} : {} messages capturés sur [{}, {}]",
                sessionId, messages.size(), from, to);

        String prompt = SummaryPromptTemplate.build(messages);
        OllamaClient client = new OllamaClient(ai.endpoint(), Duration.ofSeconds(ai.timeoutSeconds()));
        Optional<String> generated = client.generate(ai.model(), prompt);
        if (generated.isEmpty()) {
            log.warn("Aucun résumé généré pour session {}", sessionId);
            return Optional.empty();
        }
        Summary saved = summaries.save(sessionId, generated.get(), ai.model());
        log.info("Résumé session {} sauvé id={} en {} ms",
                sessionId, saved.id(), System.currentTimeMillis() - t0);
        return Optional.of(saved);
    }
}
