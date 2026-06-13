package com.jmix.tool.impl.llm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Storage abstraction for LLM cache entries.
 */
public interface LLMCacheStore {

    Optional<LlmCacheEntry> find(String key) throws IOException;

    void put(LlmCacheEntry entry) throws IOException;

    Path entryPath(String key);
}
