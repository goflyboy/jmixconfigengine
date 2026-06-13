package com.jmix.tool.impl.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.tool.impl.ModelGenneratorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

public class LlmCacheTest {

    @TempDir
    Path tempDir;

    @Test
    public void testReadThroughCacheHitDoesNotCallDelegate() throws Exception {
        LlmModelProfile profile = qwenProfile();
        FileLLMCacheStore store = new FileLLMCacheStore(tempDir);
        put(store, profile, "system", "user", "cached-response");
        CountingInvoker delegate = new CountingInvoker("fresh-response");
        CachedLLMInvoker invoker = cached(delegate, store, LlmCacheMode.READ_THROUGH, profile);

        String response = invoker.generate("system", "user");

        assertEquals("cached-response", response);
        assertEquals(0, delegate.callCount());
        assertEquals(LlmCacheStatus.HIT, invoker.lastInvocationTrace().cacheStatus());
    }

    @Test
    public void testReadThroughCacheMissWritesEntry() throws Exception {
        LlmModelProfile profile = qwenProfile();
        FileLLMCacheStore store = new FileLLMCacheStore(tempDir);
        CountingInvoker delegate = new CountingInvoker("fresh-response");
        CachedLLMInvoker invoker = cached(delegate, store, LlmCacheMode.READ_THROUGH, profile);

        String response = invoker.generate("system", "user");

        String key = key("system", "user", profile);
        assertEquals("fresh-response", response);
        assertEquals(1, delegate.callCount());
        assertTrue(store.find(key).isPresent());
        assertEquals(LlmCacheStatus.MISS, invoker.lastInvocationTrace().cacheStatus());
    }

    @Test
    public void testForceRefreshIgnoresExistingEntry() throws Exception {
        LlmModelProfile profile = qwenProfile();
        FileLLMCacheStore store = new FileLLMCacheStore(tempDir);
        put(store, profile, "system", "user", "old-response");
        CountingInvoker delegate = new CountingInvoker("new-response");
        CachedLLMInvoker invoker = cached(delegate, store, LlmCacheMode.REFRESH, profile);

        String response = invoker.generate("system", "user");

        String key = key("system", "user", profile);
        assertEquals("new-response", response);
        assertEquals(1, delegate.callCount());
        assertEquals("new-response", store.find(key).orElseThrow().response().text());
        assertEquals(LlmCacheStatus.REFRESHED, invoker.lastInvocationTrace().cacheStatus());
    }

    @Test
    public void testDisableCacheDoesNotReadOrWrite() throws Exception {
        LlmModelProfile profile = qwenProfile();
        FileLLMCacheStore store = new FileLLMCacheStore(tempDir);
        put(store, profile, "system", "user", "cached-response");
        CountingInvoker delegate = new CountingInvoker("real-response");
        CachedLLMInvoker invoker = cached(delegate, store, LlmCacheMode.DISABLED, profile);

        String response = invoker.generate("system", "user");

        String key = key("system", "user", profile);
        assertEquals("real-response", response);
        assertEquals(1, delegate.callCount());
        assertEquals("cached-response", store.find(key).orElseThrow().response().text());
        assertEquals(LlmCacheStatus.DISABLED, invoker.lastInvocationTrace().cacheStatus());
    }

    @Test
    public void testDifferentModelIdentityUsesDifferentCacheKey() {
        String deepseekKey = key("system", "user", deepseekProfile());
        String qwenKey = key("system", "user", qwenProfile());

        assertNotEquals(deepseekKey, qwenKey);
    }

    @Test
    public void testResolveQwenAndDefaultTags() {
        Properties properties = new Properties();
        properties.setProperty("llm.default.model", "qwen");
        properties.setProperty("llm.model.qwen.provider", "openai-compatible");
        properties.setProperty("llm.model.qwen.apiKeyEnv", "QWEN_API_KEY");
        properties.setProperty("llm.model.qwen.baseUrl", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        properties.setProperty("llm.model.qwen.modelName", "qwen-plus");
        LlmModelRegistry registry = LlmModelRegistry.from(properties);

        assertEquals("qwen", registry.resolve("qwen").tag());
        assertEquals("qwen-plus", registry.resolve("default").modelName());
        assertThrows(ModelGenneratorException.class, () -> registry.resolve("QWEN"));
    }

    @Test
    public void testCacheOnlyFailsExplicitlyInP0() {
        CachedLLMInvoker invoker = cached(
                new CountingInvoker("response"),
                new FileLLMCacheStore(tempDir),
                LlmCacheMode.CACHE_ONLY,
                qwenProfile());

        assertThrows(UnsupportedOperationException.class, () -> invoker.generate("system", "user"));
    }

    @Test
    public void testApiKeyIsReadFromEnvironmentOnlyOnGenerate() {
        LlmModelProfile profile = new LlmModelProfile(
                "qwen",
                "openai-compatible",
                "RFC0017_MISSING_API_KEY_DO_NOT_SET",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen-plus",
                0.7,
                4000,
                "/chat/completions");
        LLMInvokerImpl invoker = new LLMInvokerImpl(profile);

        ModelGenneratorException error = assertThrows(
                ModelGenneratorException.class,
                () -> invoker.generate("system", "user"));

        assertTrue(error.getMessage().contains("RFC0017_MISSING_API_KEY_DO_NOT_SET"));
    }

    private CachedLLMInvoker cached(
            CountingInvoker delegate,
            FileLLMCacheStore store,
            LlmCacheMode mode,
            LlmModelProfile profile) {
        LlmRuntimeOptions runtime = new LlmRuntimeOptions(profile.tag(), mode, tempDir);
        return new CachedLLMInvoker(delegate, store, runtime, profile);
    }

    private void put(
            FileLLMCacheStore store,
            LlmModelProfile profile,
            String systemMessage,
            String userMessage,
            String response) throws Exception {
        String key = key(systemMessage, userMessage, profile);
        String now = Instant.now().toString();
        store.put(new LlmCacheEntry(
                LlmCacheKeyBuilder.SCHEMA_VERSION,
                key,
                "TEST",
                profile.tag(),
                profile.identity(),
                now,
                now,
                0,
                new LlmCacheEntry.Request(systemMessage, userMessage),
                new LlmCacheEntry.Response(response),
                new LlmCacheEntry.Diagnostics(0, "test")));
    }

    private String key(String systemMessage, String userMessage, LlmModelProfile profile) {
        return new LlmCacheKeyBuilder().key(systemMessage, userMessage, profile);
    }

    private LlmModelProfile qwenProfile() {
        return new LlmModelProfile(
                "qwen",
                "openai-compatible",
                "QWEN_API_KEY",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen-plus",
                0.7,
                4000,
                "/chat/completions");
    }

    private LlmModelProfile deepseekProfile() {
        return new LlmModelProfile(
                "deepseek",
                "openai-compatible",
                "DEEPSEEK_API_KEY",
                "https://api.deepseek.com",
                "deepseek-v4-pro",
                0.3,
                4000,
                "/chat/completions");
    }

    private static final class CountingInvoker implements LLMInvoker {

        private final String response;
        private int callCount;

        private CountingInvoker(String response) {
            this.response = response;
        }

        @Override
        public String generate(String systemMessage, String userMessage) {
            callCount++;
            return response;
        }

        @Override
        public String getConfigInfo() {
            return "counting";
        }

        private int callCount() {
            return callCount;
        }
    }
}
