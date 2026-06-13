package com.jmix.tool.impl.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Optional;

/**
 * Local JSON-file implementation of the LLM cache store.
 */
@Slf4j
public final class FileLLMCacheStore implements LLMCacheStore {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path cacheDir;

    public FileLLMCacheStore(Path cacheDir) {
        if (cacheDir == null) {
            throw new IllegalArgumentException("cacheDir must not be null");
        }
        this.cacheDir = cacheDir;
    }

    @Override
    public Optional<LlmCacheEntry> find(String key) throws IOException {
        Path file = entryPath(key);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            LlmCacheEntry entry = OBJECT_MAPPER.readValue(file.toFile(), LlmCacheEntry.class);
            if (entry.response() == null || entry.response().text() == null
                    || entry.response().text().isBlank()) {
                log.warn("Ignoring LLM cache entry with blank response: {}", file);
                return Optional.empty();
            }
            return Optional.of(entry);
        } catch (Exception e) {
            quarantine(file, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(LlmCacheEntry entry) throws IOException {
        Path file = entryPath(entry.key());
        Files.createDirectories(file.getParent());
        Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
        OBJECT_MAPPER.writeValue(tempFile.toFile(), entry);
        moveAtomically(tempFile, file);
        appendIndex(entry, file);
    }

    @Override
    public Path entryPath(String key) {
        String normalized = normalizeKey(key);
        String prefix = normalized.substring(0, 2);
        return cacheDir.resolve("entries").resolve(prefix).resolve(normalized + ".json");
    }

    private void appendIndex(LlmCacheEntry entry, Path entryFile) {
        try {
            Files.createDirectories(cacheDir);
            Path indexFile = cacheDir.resolve("index.jsonl");
            String relativeEntry = cacheDir.relativize(entryFile).toString().replace('\\', '/');
            String line = "{\"key\":\"" + escape(entry.key())
                    + "\",\"stage\":\"" + escape(entry.stage())
                    + "\",\"modelTag\":\"" + escape(entry.modelTag())
                    + "\",\"modelIdentity\":\"" + escape(entry.modelIdentity())
                    + "\",\"createdAt\":\"" + escape(entry.createdAt())
                    + "\",\"entryFile\":\"" + escape(relativeEntry)
                    + "\"}" + System.lineSeparator();
            Files.writeString(indexFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("Failed to append LLM cache index: {}", e.getMessage());
        }
    }

    private void quarantine(Path file, Exception cause) {
        try {
            Path quarantineDir = cacheDir.resolve("quarantine");
            Files.createDirectories(quarantineDir);
            Path target = quarantineDir.resolve(file.getFileName() + "." + Instant.now().toEpochMilli());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            log.warn("Moved unreadable LLM cache entry to quarantine: {}", target);
        } catch (Exception moveError) {
            log.warn("Failed to read LLM cache entry {}: {}", file, cause.getMessage());
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String normalizeKey(String key) {
        if (key == null || !key.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("LLM cache key must be a SHA-256 hex string");
        }
        return key.toLowerCase();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
