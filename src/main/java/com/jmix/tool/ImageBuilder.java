package com.jmix.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;

/**
 * Image Builder - GPT Image 2 图片生成器
 * 
 * 使用 Java 原生 HttpClient 调用 OpenAI 兼容 API 生成图片
 * 
 * @since 2025-05-03
 */
@Slf4j
public class ImageBuilder {

    private static final String DEFAULT_BASE_URL = "https://code.codingplay.top";
    private static final String MODEL_NAME = "gpt-image-2";
    private static final int TIMEOUT_SECONDS = 300; // 5分钟超时

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;

    /**
     * 使用环境变量 OPENAI_API_KEY 创建 ImageBuilder
     */
    public ImageBuilder() {
        this(System.getenv("OPENAI_API_KEY"), DEFAULT_BASE_URL);
    }

    /**
     * 使用指定的 API Key 和 Base URL 创建 ImageBuilder
     * 
     * @param apiKey  OpenAI API Key
     * @param baseUrl API 基础 URL
     */
    public ImageBuilder(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OPENAI_API_KEY is required");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl.trim() : DEFAULT_BASE_URL;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        log.info("ImageBuilder initialized with baseUrl: {}", this.baseUrl);
    }

    /**
     * 生成图片并保存到指定路径
     */
    public Path generateImage(String prompt, String outputPath) throws Exception {
        return generateImage(prompt, Paths.get(outputPath));
    }

    /**
     * 生成图片并保存到指定路径
     */
    public Path generateImage(String prompt, Path outputPath) throws Exception {
        log.info("Generating image with prompt length: {}", prompt.length());

        String requestBody = buildRequestBody(prompt);
        ImageResponse response = sendRequest(requestBody);
        saveImage(response, outputPath);

        log.info("Image saved to: {}", outputPath.toAbsolutePath());
        return outputPath;
    }

    /**
     * 生成图片并返回 Base64 编码
     */
    public String generateImageAsBase64(String prompt) throws Exception {
        log.info("Generating image as Base64 with prompt length: {}", prompt.length());

        String requestBody = buildRequestBody(prompt);
        ImageResponse response = sendRequest(requestBody);

        if (response.data != null && !response.data.isEmpty()) {
            return response.data.get(0).b64Json;
        }
        throw new RuntimeException("No image data returned from API");
    }

    private String buildRequestBody(String prompt) {
        return """
                {
                    "model": "%s",
                    "prompt": %s,
                    "n": 1,
                    "size": "1024x1024",
                    "response_format": "b64_json"
                }
                """.formatted(MODEL_NAME, toJsonString(prompt));
    }

    private ImageResponse sendRequest(String requestBody) throws Exception {
        String endpoint = baseUrl + "/v1/images/generations";
        log.info("Sending request to: {}", endpoint);

        long startTime = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("HTTP request started...");

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Request completed in {} ms, status: {}", elapsed, response.statusCode());

            if (response.statusCode() != 200) {
                throw new RuntimeException("API error: " + response.statusCode() + " - " + response.body());
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("Empty response from API");
            }

            return parseResponse(responseBody);

        } catch (java.net.http.HttpTimeoutException e) {
            throw new RuntimeException("Request timeout after " + TIMEOUT_SECONDS + " seconds", e);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Network error: " + e.getMessage(), e);
        }
    }

    private ImageResponse parseResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseBody, ImageResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse API response: " + e.getMessage(), e);
        }
    }

    private void saveImage(ImageResponse response, Path outputPath) throws IOException {
        if (response.data == null || response.data.isEmpty()) {
            throw new RuntimeException("No image data in response");
        }

        String b64Json = response.data.get(0).b64Json;
        if (b64Json == null || b64Json.isEmpty()) {
            throw new RuntimeException("No base64 image data in response");
        }

        byte[] imageBytes = Base64.getDecoder().decode(b64Json);

        Path parentDir = outputPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.write(outputPath, imageBytes);
        log.info("Image saved: {} bytes", imageBytes.length);
    }

    private String toJsonString(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    /**
     * API 响应数据结构
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageResponse {
        public int created;
        public java.util.List<ImageData> data;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ImageData {
            public String url;

            @JsonProperty("b64_json")
            public String b64Json;

            @JsonProperty("revised_prompt")
            public String revisedPrompt;
        }
    }

}
