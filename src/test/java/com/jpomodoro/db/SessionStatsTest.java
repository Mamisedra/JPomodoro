package com.jpomodoro.db;

import com.jpomodoro.TestSupport;
import com.jpomodoro.timer.SessionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionStatsTest {

    private SessionRepository sessions;

    @BeforeEach
    void setup() throws Exception {
        resetDb();
        Database.open(TestSupport.tempPaths().database());
        sessions = new SessionRepository();
    }

    @AfterEach
    void teardown() throws Exception { resetDb(); }

    @Test
    void listLastReturnsNewestFirst() {
        long a = sessions.start(SessionType.FOCUS, null);
        sessions.complete(a);
        long b = sessions.start(SessionType.SHORT_BREAK, null);
        sessions.complete(b);
        long c = sessions.start(SessionType.FOCUS, null);
        sessions.complete(c);

        List<SessionView> all = sessions.listLast(10);
        assertThat(all).extracting(SessionView::id).containsExactly(c, b, a);
    }

    @Test
    void countFocusPerDayCountsCompletedFocusOnly() {
        long a = sessions.start(SessionType.FOCUS, null);
        sessions.complete(a);
        long b = sessions.start(SessionType.FOCUS, null);
        sessions.complete(b);
        long c = sessions.start(SessionType.SHORT_BREAK, null);
        sessions.complete(c);
        long d = sessions.start(SessionType.FOCUS, null);
        sessions.cancel(d);

        LocalDate today = LocalDate.now();
        int[] counts = sessions.countFocusPerDay(today.minusDays(2), today);
        assertThat(counts).hasSize(3);
        assertThat(counts[2]).isEqualTo(2);
    }

    @Test
    void streakDaysReturnsOneWhenTodayHasFocus() {
        long a = sessions.start(SessionType.FOCUS, null);
        sessions.complete(a);
        assertThat(sessions.streakDays()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void streakDaysReturnsZeroWhenNoFocusEver() {
        assertThat(sessions.streakDays()).isEqualTo(0);
    }

    private static void resetDb() throws Exception {
        Method m = Database.class.getDeclaredMethod("resetForTests");
        m.setAccessible(true);
        m.invoke(null);
    }
}
