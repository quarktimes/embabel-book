package com.example.library.service;

import com.example.library.domain.ParsedQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * DeepSeek API 服务 — 将用户自然语言解析为结构化查询。
 * <p>
 * 双路径设计：
 * 1. API 路径：调 DeepSeek LLM，返回 JSON
 * 2. 降级路径：API Key 为空或调用失败时，走关键词匹配
 */
@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final double temperature;

    public DeepSeekService(
            RestTemplate restTemplate,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.temperature:0.1}") double temperature) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.temperature = temperature;
    }

    /**
     * 将用户自然语言查询解析为结构化 ParsedQuery。
     * 优先走 DeepSeek API，API 不可用时降级为关键词匹配。
     */
    public ParsedQuery parseQuery(String userQuery) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("DeepSeek API key not configured, using fallback parser");
            return fallbackParse(userQuery);
        }
        try {
            return callApi(userQuery);
        } catch (Exception e) {
            log.warn("DeepSeek API call failed, using fallback parser: {}", e.getMessage());
            return fallbackParse(userQuery);
        }
    }

    private ParsedQuery callApi(String userQuery) {
        try {
            var requestBody = buildRequestBody(userQuery);
            var response = restTemplate.postForObject(
                    baseUrl + "/v1/chat/completions",
                    requestBody,
                    String.class);

            var root = MAPPER.readTree(response);
            var content = root.path("choices").get(0).path("message").path("content").asText();

            // LLM 可能返回 Markdown 代码块，需要清理
            content = content.replaceAll("```json\\s*|```\\s*", "").trim();

            return MAPPER.readValue(content, ParsedQuery.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DeepSeek API response", e);
        }
    }

    private Map<String, Object> buildRequestBody(String userQuery) {
        return Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "system", "content", buildSystemPrompt()),
                        Map.of("role", "user", "content", userQuery)
                },
                "temperature", temperature
        );
    }

    private String buildSystemPrompt() {
        return """
                你是一个图书查询解析助手。请将用户的自然语言查询解析为结构化信息。
                返回 JSON 格式，不要包含 Markdown 代码块标记。

                字段说明：
                - category: 图书分类（科幻、推理、计算机、文学、历史...），没有则为 null
                - keyword: 关键词，没有则为 null
                - author: 作者名，没有则为 null

                示例：
                用户：我想借一些科幻小说
                返回：{"category":"科幻","keyword":null,"author":null}

                用户：有没有东野圭吾的推理小说
                返回：{"category":"推理","keyword":null,"author":"东野圭吾"}

                用户：推荐机器学习的书
                返回：{"category":"计算机","keyword":"机器学习","author":null}
                """;
    }

    // ─── 降级解析：关键词匹配 ───

    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "科幻|推理|计算机|编程|文学|历史|哲学|经济|心理|教育");

    private static final Pattern AUTHOR_PATTERN = Pattern.compile(
            "(?:刘慈欣|赫伯特|阿西莫夫|东野圭吾|马尔克斯|斋藤|赫拉利|李航)");

    ParsedQuery fallbackParse(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return new ParsedQuery(null, null, null);
        }

        // 提取作者
        var authorMatcher = AUTHOR_PATTERN.matcher(userQuery);
        String author = authorMatcher.find() ? authorMatcher.group() : null;

        // 提取分类
        var categoryMatcher = CATEGORY_PATTERN.matcher(userQuery);
        String category = categoryMatcher.find() ? categoryMatcher.group() : null;

        // 剩余内容作为关键词
        String keyword = null;
        if (author == null && category == null) {
            keyword = userQuery;
        }

        return new ParsedQuery(category, keyword, author);
    }
}
