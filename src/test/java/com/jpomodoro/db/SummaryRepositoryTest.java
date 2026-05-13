package com.jpomodoro.db;

import com.jpomodoro.TestSupport;
import com.jpomodoro.model.Summary;
import com.jpomodoro.timer.SessionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryRepositoryTest {

    private SessionRepository sessions;
    private SummaryRepository summaries;
    private FeedbackRepository feedbacks;

    @BeforeEach
    void setup() throws Exception {
        resetDb();
        Database.open(TestSupport.tempPaths().database());
        sessions = new SessionRepository();
        summaries = new SummaryRepository();
        feedbacks = new FeedbackRepository();
    }

    @AfterEach
    void teardown() throws Exception { resetDb(); }

    @Test
    void saveAndRetrieveSummary() {
        long sessionId = sessions.start(SessionType.FOCUS, null);
        sessions.complete(sessionId);

        Summary saved = summaries.save(sessionId, "Travail sur tests JUnit", "qwen2.5:7b");

        assertThat(saved.id()).isPositive();
        assertThat(saved.sessionId()).isEqualTo(sessionId);
        assertThat(saved.model()).isEqualTo("qwen2.5:7b");

        Optional<Summary> retrieved = summaries.findBySession(sessionId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().content()).isEqualTo("Travail sur tests JUnit");
    }

    @Test
    void feedbackPersists() {
        long sessionId = sessions.start(SessionType.FOCUS, null);
        Summary s = summaries.save(sessionId, "X", "model");

        var fb = feedbacks.save(s.id(), true);
        assertThat(fb.summaryId()).isEqualTo(s.id());
        assertThat(fb.isPositive()).isTrue();
    }

    @Test
    void findBySessionReturnsEmptyWhenAbsent() {
        long sessionId = sessions.start(SessionType.FOCUS, null);
        assertThat(summaries.findBySession(sessionId)).isEmpty();
    }

    private static void resetDb() throws Exception {
        Method m = Database.class.getDeclaredMethod("resetForTests");
        m.setAccessible(true);
        m.invoke(null);
    }
}
