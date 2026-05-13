package com.jpomodoro.ui.onboarding;

import com.googlecode.lanterna.screen.Screen;
import com.jpomodoro.config.AppConfig;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.ui.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OnboardingFlow {

    private static final Logger log = LoggerFactory.getLogger(OnboardingFlow.class);
    private static final Pattern MODEL_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    private OnboardingFlow() {}

    public static void run(Screen screen, ConfigService config) throws IOException {
        screen.startScreen();
        try {
            Modal.info(screen, "Bienvenue dans JPomodoro", List.of(
                    "Deux questions rapides pour démarrer.",
                    "Tout sera modifiable plus tard dans l'onglet Réglages.",
                    "",
                    "Astuce : Esc à tout moment garde la valeur courante."
            ));

            step1Schedule(screen, config);
            step2Ai(screen, config);

            config.update(c -> withOnboarded(c, true));

            Modal.info(screen, "Tout est prêt", List.of(
                    "Bonne séance !",
                    "",
                    "Raccourcis : [s] start  [Tab] vue  [m] auto/manuel  [q] quit"
            ));
        } finally {
            screen.stopScreen();
        }
    }

    private static void step1Schedule(Screen screen, ConfigService config) throws IOException {
        Modal.info(screen, "1/2 · Horaires de travail", List.of(
                "Saisissez vos plages au format HH:mm.",
                "Le mode AUTO empêchera de démarrer un focus hors créneaux."
        ));

        String ms = ask(screen, "Matin début (HH:mm)", config.get().schedule().morningStart());
        String me = ask(screen, "Matin fin (HH:mm)", config.get().schedule().morningEnd());
        String as = ask(screen, "Après-midi début (HH:mm)", config.get().schedule().afternoonStart());
        String ae = ask(screen, "Après-midi fin (HH:mm)", config.get().schedule().afternoonEnd());

        config.update(c -> new AppConfig(
                c.timer(),
                new AppConfig.ScheduleSettings(ms, me, as, ae, c.schedule().mode()),
                c.ai(), c.notification(), c.webhook(), c.appearance(), c.onboarded()));
    }

    private static void step2Ai(Screen screen, ConfigService config) throws IOException {
        String endpoint = config.get().ai().endpoint();
        Modal.info(screen, "2/2 · IA Ollama", List.of(
                "Vérification d'Ollama en local sur " + endpoint + " …",
                "L'IA résume ton focus à la pause (optionnel)."
        ));

        Optional<List<String>> models = pingOllama(endpoint);

        if (models.isEmpty()) {
            Modal.info(screen, "IA indisponible", List.of(
                    "Ollama injoignable à " + endpoint + ".",
                    "L'IA est désactivée. Active-la plus tard dans Réglages."
            ));
            config.update(c -> withAi(c, false, c.ai().model()));
            return;
        }

        List<String> options = models.get();
        if (options.isEmpty()) {
            Modal.info(screen, "Aucun modèle Ollama", List.of(
                    "Ollama répond mais n'a aucun modèle installé.",
                    "Lance par exemple : `ollama pull qwen2.5:7b`",
                    "L'IA est désactivée pour le moment."
            ));
            config.update(c -> withAi(c, false, c.ai().model()));
            return;
        }

        Optional<Integer> picked = Modal.choose(screen, "Choisis un modèle Ollama", options);
        if (picked.isEmpty()) {
            config.update(c -> withAi(c, false, c.ai().model()));
            return;
        }
        String model = options.get(picked.get());
        config.update(c -> withAi(c, true, model));
    }

    private static String ask(Screen screen, String label, String current) throws IOException {
        String raw = Modal.readLine(screen, label + " : ", current);
        if (raw == null || raw.isBlank()) return current;
        try {
            java.time.LocalTime.parse(raw.trim());
            return raw.trim();
        } catch (Exception e) {
            return current;
        }
    }

    private static Optional<List<String>> pingOllama(String endpoint) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return Optional.empty();
            List<String> names = new ArrayList<>();
            Matcher m = MODEL_NAME.matcher(resp.body());
            while (m.find()) names.add(m.group(1));
            return Optional.of(names);
        } catch (Exception e) {
            log.warn("Ollama ping échec : {}", e.toString());
            return Optional.empty();
        }
    }

    private static AppConfig withAi(AppConfig c, boolean enabled, String model) {
        return new AppConfig(c.timer(), c.schedule(),
                new AppConfig.AiSettings(enabled, model, c.ai().endpoint(), c.ai().timeoutSeconds()),
                c.notification(), c.webhook(), c.appearance(), c.onboarded());
    }

    private static AppConfig withOnboarded(AppConfig c, boolean v) {
        return new AppConfig(c.timer(), c.schedule(), c.ai(), c.notification(), c.webhook(), c.appearance(), v);
    }
}
