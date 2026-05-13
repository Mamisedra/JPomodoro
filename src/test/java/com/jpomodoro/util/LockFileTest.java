package com.jpomodoro.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockFileTest {

    @Test
    void secondAcquireFailsWhileFirstHeld(@TempDir Path tmp) throws Exception {
        Path path = tmp.resolve(".lock");
        try (LockFile first = LockFile.acquire(path)) {
            assertThat(first.path()).isEqualTo(path);
            assertThatThrownBy(() -> LockFile.acquire(path))
                    .isInstanceOf(LockFile.LockUnavailableException.class);
        }
    }

    @Test
    void canReacquireAfterRelease(@TempDir Path tmp) throws Exception {
        Path path = tmp.resolve(".lock");
        try (LockFile first = LockFile.acquire(path)) {}
        try (LockFile second = LockFile.acquire(path)) {
            assertThat(second).isNotNull();
        }
    }
}
