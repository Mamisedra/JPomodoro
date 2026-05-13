package com.jpomodoro.db;

import com.jpomodoro.TestSupport;
import com.jpomodoro.model.Priority;
import com.jpomodoro.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRepositoryTest {

    private TaskRepository repo;

    @BeforeEach
    void setup() throws Exception {
        resetDb();
        Database.open(TestSupport.tempPaths().database());
        repo = new TaskRepository();
    }

    @AfterEach
    void teardown() throws Exception {
        resetDb();
    }

    @Test
    void addCreatesTaskWithNormalPriority() {
        Task t = repo.add("courses");
        assertThat(t.priority()).isEqualTo(Priority.NORMAL);
        assertThat(t.done()).isFalse();
        assertThat(t.estimatedPomodoros()).isNull();
        assertThat(t.deletedAt()).isNull();
    }

    @Test
    void listOpenExcludesSoftDeleted() {
        Task a = repo.add("a");
        repo.add("b");

        repo.delete(a.id());

        List<Task> remaining = repo.listOpen();
        assertThat(remaining).extracting(Task::title).containsExactly("b");
    }

    @Test
    void cyclePriorityNormalToHighToLowToNormal() {
        Task t = repo.add("x");
        assertThat(repo.cyclePriority(t.id())).isEqualTo(Priority.HIGH);
        assertThat(repo.cyclePriority(t.id())).isEqualTo(Priority.LOW);
        assertThat(repo.cyclePriority(t.id())).isEqualTo(Priority.NORMAL);
    }

    @Test
    void listOpenOrdersByDoneThenPriority() {
        Task a = repo.add("low");
        Task b = repo.add("high");
        repo.add("normal");
        repo.cyclePriority(a.id());
        repo.cyclePriority(a.id());
        assertThat(repo.cyclePriority(b.id())).isEqualTo(Priority.HIGH);

        List<Task> list = repo.listOpen();

        assertThat(list).extracting(Task::title).containsExactly("high", "normal", "low");
    }

    @Test
    void setEstimateAcceptsAndClears() {
        Task t = repo.add("x");
        repo.setEstimate(t.id(), 5);
        assertThat(repo.listOpen().get(0).estimatedPomodoros()).isEqualTo(5);

        repo.setEstimate(t.id(), null);
        assertThat(repo.listOpen().get(0).estimatedPomodoros()).isNull();

        repo.setEstimate(t.id(), 0);
        assertThat(repo.listOpen().get(0).estimatedPomodoros()).isNull();
    }

    @Test
    void softDeleteIsIdempotent() {
        Task t = repo.add("x");
        repo.delete(t.id());
        repo.delete(t.id());
        assertThat(repo.listOpen()).isEmpty();
    }

    private static void resetDb() throws Exception {
        Method m = Database.class.getDeclaredMethod("resetForTests");
        m.setAccessible(true);
        m.invoke(null);
    }
}
