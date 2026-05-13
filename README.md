# JPomodoro

Pomodoro TUI Java/Lanterna offline-first avec tâches, schedule, IA résumés Ollama, notifications cross-OS, historique et export CSV.

## Démarrage

```bash
mvn package -DskipTests
java -jar target/pomodoro.jar
```

Pré-requis : Java 17+. Maven pour build.

Au premier lancement, onboarding 2 étapes : horaires de travail + détection Ollama (optionnelle).

## Raccourcis clavier

Touche `?` à tout moment ouvre l'aide complète.

### Timer
| Touche | Action |
|---|---|
| `s` | démarrer / reprendre |
| `p` | pause |
| `r` | reset |
| `n` | skip session |
| `m` | toggle AUTO / MANUEL (schedule) |

### Tâches (onglet Timer)
| Touche | Action |
|---|---|
| `a` | ajouter |
| `d` | supprimer (soft-delete) |
| `espace` | activer / désactiver |
| `Enter` | cocher / décocher |
| `!` | cycler priorité (NORMAL → HIGH → LOW) |
| `e` | éditer estimation |

### Navigation
| Touche | Action |
|---|---|
| `Tab` | onglet suivant |
| `1` / `2` / `3` | Timer / Historique / Réglages |
| `↑ ↓` | naviguer |

### Global
| Touche | Action |
|---|---|
| `x` | export CSV |
| `?` | aide |
| `q` | quitter |

## Configuration

Fichier TOML : `~/.pomodoro/config.toml`. Sections :

- `[timer]` durées focus / pause courte / pause longue, cycles avant pause longue
- `[schedule]` plages matin/après-midi + mode AUTO/MANUAL
- `[ai]` toggle, modèle Ollama, endpoint, timeout
- `[notification]` toggle + son (bell/chime/ding/soft/none)
- `[webhook]` URL Google Chat + toggle
- `[appearance]` palette (default / mono / synthwave)

Édition possible via onglet Réglages dans l'app.

## IA Ollama

Si Ollama tourne en local (`ollama serve` ou Ollama.app) et un modèle est pull, JPomodoro génère automatiquement un résumé de chaque focus en lisant les JSONL de `~/.claude/projects/`. Affiché en modal à la pause avec feedback 👍/👎.

Désactivable via Réglages → "IA activée".

### Modèles recommandés (rapidité ↔ qualité)

| Modèle | Pull | RAM | Vitesse | Note |
|---|---|---|---|---|
| `llama3.2:3b` ⭐ | `ollama pull llama3.2:3b` | ~2 GB | rapide | Meilleur compromis fr |
| `gemma2:2b` | `ollama pull gemma2:2b` | ~1.5 GB | très rapide | Google, ultra léger |
| `qwen2.5:7b` | `ollama pull qwen2.5:7b` | ~5 GB | lent | Qualité max |
| `phi3.5` | `ollama pull phi3.5` | ~2.5 GB | moyen | Microsoft, factuel |

Sélection dans Réglages → "Modèle IA".

### Stocker les modèles sur un disque externe

Modèles Ollama dans `~/.ollama/models/`. Pour économiser l'espace SSD :

```bash
# 1. Quitter Ollama (icône menubar → Quit, ou kill processes)
pkill -f Ollama

# 2. Déplacer vers disque externe
mkdir -p /Volumes/DD-2/ollama-models
mv ~/.ollama/models/* /Volumes/DD-2/ollama-models/
rmdir ~/.ollama/models
ln -s /Volumes/DD-2/ollama-models ~/.ollama/models

# 3. Relancer Ollama
open -a Ollama
ollama list  # vérifie modèles toujours visibles
```

Note : 1er appel après restart est lent (load modèle depuis USB ~30-50s). Appels suivants rapides (≤ 1s) grâce au keep-alive Ollama.

Si DD-2 démonté → Ollama ne trouve plus les modèles. Re-mount avant `pomodoro`.

## Notifications

- macOS : `osascript` toast + audio Java
- Linux : `notify-send` + audio Java
- Windows : no-op (fallback bell terminal)

Sons preset embedded : bell, chime, ding, soft, none.

## Webhook Google Chat

Coller l'URL de webhook d'un espace dans Réglages → "Webhook URL", activer → résumés envoyés automatiquement à chaque pause.

## Données

- DB SQLite : `~/.pomodoro/data.db` (tasks, sessions, summaries, feedback_entries, schema_migrations)
- Logs rolling : `~/.pomodoro/logs/pomodoro.log` (10 MB × 5)
- Lock file : `~/.pomodoro/.lock` (empêche double instance)
- Exports CSV : `~/.pomodoro/export-YYYYMMDD-HHmm.csv`

## Stack

Java 17 · Lanterna 3.1.2 · SQLite JDBC · tomlj · Logback · Jackson · JUnit 5 · AssertJ
