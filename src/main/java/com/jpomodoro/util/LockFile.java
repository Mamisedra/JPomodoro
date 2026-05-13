package com.jpomodoro.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class LockFile implements AutoCloseable {

    private final FileChannel channel;
    private final FileLock lock;
    private final Path path;

    private LockFile(Path path, FileChannel channel, FileLock lock) {
        this.path = path;
        this.channel = channel;
        this.lock = lock;
    }

    public static LockFile acquire(Path path) throws LockUnavailableException, IOException {
        Files.createDirectories(path.getParent());
        FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        FileLock lock;
        try {
            lock = channel.tryLock();
        } catch (Exception e) {
            channel.close();
            throw new LockUnavailableException("Lock indisponible : " + e.getMessage());
        }
        if (lock == null) {
            channel.close();
            throw new LockUnavailableException("Une autre instance utilise déjà " + path);
        }
        return new LockFile(path, channel, lock);
    }

    public Path path() { return path; }

    @Override
    public void close() {
        try {
            if (lock != null && lock.isValid()) lock.release();
        } catch (IOException ignored) {}
        try {
            if (channel != null && channel.isOpen()) channel.close();
        } catch (IOException ignored) {}
    }

    public static final class LockUnavailableException extends Exception {
        public LockUnavailableException(String message) { super(message); }
    }
}
