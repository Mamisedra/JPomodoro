package com.jpomodoro.ai;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SummaryPromptTemplate {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private SummaryPromptTemplate() {}

    public static String build(List<ParsedMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un assistant qui résume l'activité d'un développeur. ");
        sb.append("Produis un résumé en 3 à 5 phrases, en français, factuel, sans invention. ");
        sb.append("Ne mentionne pas le format JSON ni les outils internes.\n\n");
        sb.append("## Activité récente\n\n");
        if (messages.isEmpty()) {
            sb.append("(Aucun message capturé sur la fenêtre.)\n");
        } else {
            for (ParsedMessage m : messages) {
                sb.append("[").append(HM.format(m.timestamp())).append("] ");
                sb.append(m.role()).append(" : ");
                sb.append(truncate(m.content(), 400)).append("\n");
            }
        }
        sb.append("\nRésumé :");
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }
}
