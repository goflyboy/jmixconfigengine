package com.jmix.tool.impl.llm;

import com.jmix.tool.impl.ModelGenneratorException;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;

/**
 * LLM invoker decorator with local file cache support.
 */
@Slf4j
public final class CachedLLMInvoker implements LLMInvoker, LlmInvocationTraceProvider {

    private final LLMInvoker delegate;
    private final LLMCacheStore store;
    private final LlmRuntimeOptions runtime;
    private final LlmModelProfile profile;
    private final LlmCacheKeyBuilder keyBuilder;
    private final ThreadLocal<LlmInvocationTrace> lastTrace = ThreadLocal.withInitial(LlmInvocationTrace::empty);

    public CachedLLMInvoker(LLMInvoker delegate, LLMCacheStore store, LlmRuntimeOptions runtime) {
        this(delegate, store, safeRuntime(runtime), LlmModelRegistry.load().resolve(safeRuntime(runtime).modelTag()));
    }

    public CachedLLMInvoker(
            LLMInvoker delegate,
            LLMCacheStore store,
            LlmRuntimeOptions runtime,
            LlmModelProfile profile) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.delegate = delegate;
        this.store = store;
        this.runtime = runtime == null ? LlmRuntimeOptions.defaults() : runtime;
        this.profile = profile;
        this.keyBuilder = new LlmCacheKeyBuilder();
    }

    @Override
    public String generate(String systemMessage, String userMessage) throws Exception {
        String key = keyBuilder.key(systemMessage, userMessage, profile);
        if (runtime.cacheMode() == LlmCacheMode.CACHE_ONLY) {
            throw new UnsupportedOperationException("CACHE_ONLY is not implemented in P0");
        }
        if (runtime.cacheMode() == LlmCacheMode.DISABLED) {
            String response = delegate.generate(systemMessage, userMessage);
            trace(LlmCacheStatus.DISABLED, key);
            return response;
        }
        if (runtime.cacheMode() == LlmCacheMode.READ_THROUGH) {
            Optional<LlmCacheEntry> cached = store.find(key);
            if (cached.isPresent()) {
                log.info("LLM cache hit: key={} model={}", shortKey(key), profile.tag());
                LlmCacheEntry entry = cached.get();
                tryUpdateHit(entry);
                trace(LlmCacheStatus.HIT, key);
                return entry.response().text();
            }
        }

        LlmCacheStatus status = runtime.cacheMode() == LlmCacheMode.REFRESH
                ? LlmCacheStatus.REFRESHED
                : LlmCacheStatus.MISS;
        log.info("LLM cache {}: key={} model={}", status.name().toLowerCase(), shortKey(key), profile.tag());
        long start = System.nanoTime();
        String response = delegate.generate(systemMessage, userMessage);
        long durationMillis = (System.nanoTime() - start) / 1_000_000L;
        if (response == null || response.isBlank()) {
            throw new ModelGenneratorException("LLM returned empty response");
        }
        LlmCacheEntry entry = newEntry(key, systemMessage, userMessage, response, durationMillis);
        try {
            store.put(entry);
            trace(status, key);
        } catch (Exception e) {
            log.warn("Failed to write LLM cache entry: {}", e.getMessage());
            trace(LlmCacheStatus.WRITE_FAILED, key);
        }
        return response;
    }

    @Override
    public String getConfigInfo() {
        return delegate.getConfigInfo();
    }

    @Override
    public LlmInvocationTrace lastInvocationTrace() {
        return lastTrace.get();
    }

    private LlmCacheEntry newEntry(
            String key,
            String systemMessage,
            String userMessage,
            String response,
            long durationMillis) {
        String now = Instant.now().toString();
        return new LlmCacheEntry(
                LlmCacheKeyBuilder.SCHEMA_VERSION,
                key,
                "LLM_CALL",
                profile.tag(),
                profile.identity(),
                now,
                now,
                0,
                new LlmCacheEntry.Request(systemMessage, userMessage),
                new LlmCacheEntry.Response(response),
                new LlmCacheEntry.Diagnostics(durationMillis, delegate.getConfigInfo()));
    }

    private void tryUpdateHit(LlmCacheEntry entry) {
        try {
            store.put(entry.withHit(Instant.now().toString()));
        } catch (Exception e) {
            log.warn("Failed to update LLM cache hit count: {}", e.getMessage());
        }
    }

    private void trace(LlmCacheStatus status, String key) {
        lastTrace.set(new LlmInvocationTrace(
                profile.tag(),
                profile.identity(),
                status,
                key,
                store.entryPath(key)));
    }

    private String shortKey(String key) {
        return key.length() <= 12 ? key : key.substring(0, 12);
    }

    private static LlmRuntimeOptions safeRuntime(LlmRuntimeOptions runtime) {
        return runtime == null ? LlmRuntimeOptions.defaults() : runtime;
    }
}
