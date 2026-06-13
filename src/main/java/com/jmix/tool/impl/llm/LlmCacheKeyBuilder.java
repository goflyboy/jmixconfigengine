package com.jmix.tool.impl.llm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds stable prompt-level cache keys for LLMInvoker.generate.
 */
public final class LlmCacheKeyBuilder {

    public static final int SCHEMA_VERSION = 1;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String key(String systemMessage, String userMessage, LlmModelProfile profile) {
        return sha256Hex(canonicalJson(systemMessage, userMessage, profile));
    }

    public String canonicalJson(String systemMessage, String userMessage, LlmModelProfile profile) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("schemaVersion", SCHEMA_VERSION);
            payload.put("contract", "LLMInvoker.generate");
            payload.put("modelIdentity", profile.identity());
            payload.put("systemMessage", safe(systemMessage));
            payload.put("userMessage", safe(userMessage));
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build LLM cache key payload", e);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate LLM cache key", e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
