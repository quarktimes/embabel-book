package com.example.library.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeepSeekService 契约测试")
class DeepSeekServiceTest {

    @Mock
    private RestTemplate restTemplate;

    // ─── API 路径 ───

    @Test
    @DisplayName("API 正常返回 → 解析为 ParsedQuery")
    void api_正常返回() {
        var service = new DeepSeekService(restTemplate, "sk-test", "deepseek-chat", "https://api.deepseek.com", 0.1);
        mockApiResponse("{\"category\":\"科幻\",\"keyword\":null,\"author\":null}");

        var result = service.parseQuery("我想借一些科幻小说");

        assertAll(
                () -> assertEquals("科幻", result.category()),
                () -> assertNull(result.keyword()),
                () -> assertNull(result.author())
        );
    }

    @Test
    @DisplayName("API 返回带 Markdown 标记 → 能正确清理")
    void api_返回Markdown标记_清理后解析() {
        var service = new DeepSeekService(restTemplate, "sk-test", "deepseek-chat", "https://api.deepseek.com", 0.1);
        mockApiResponse("```json\n{\"category\":\"推理\",\"keyword\":null,\"author\":\"东野圭吾\"}\n```");

        var result = service.parseQuery("东野圭吾的推理小说");

        assertEquals("推理", result.category());
        assertEquals("东野圭吾", result.author());
    }

    // ─── 降级路径 ───

    @Test
    @DisplayName("API Key 为空 → 走降级解析")
    void apiKey为空_走降级() {
        var service = new DeepSeekService(restTemplate, "", "deepseek-chat", "https://api.deepseek.com", 0.1);

        var result = service.parseQuery("我想借科幻小说");

        assertEquals("科幻", result.category());
    }

    @Test
    @DisplayName("API 调用失败（抛异常）→ 走降级解析")
    void api调用失败_走降级() {
        var service = new DeepSeekService(restTemplate, "sk-test", "deepseek-chat", "https://api.deepseek.com", 0.1);
        when(restTemplate.postForObject(any(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        var result = service.parseQuery("我想借科幻小说");

        assertEquals("科幻", result.category());
    }

    @Test
    @DisplayName("降级解析：分类识别")
    void fallback_分类识别() {
        var service = new DeepSeekService(restTemplate, "", "deepseek-chat", "https://api.deepseek.com", 0.1);

        assertAll(
                () -> assertEquals("科幻", service.parseQuery("科幻小说").category()),
                () -> assertEquals("推理", service.parseQuery("推理").category()),
                () -> assertEquals("计算机", service.parseQuery("计算机编程").category()),
                () -> assertEquals("文学", service.parseQuery("文学名著").category())
        );
    }

    @Test
    @DisplayName("降级解析：作者识别")
    void fallback_作者识别() {
        var service = new DeepSeekService(restTemplate, "", "deepseek-chat", "https://api.deepseek.com", 0.1);

        assertAll(
                () -> assertEquals("东野圭吾", service.parseQuery("东野圭吾的书").author()),
                () -> assertEquals("刘慈欣", service.parseQuery("刘慈欣的作品").author())
        );
    }

    @Test
    @DisplayName("降级解析：无法识别分类和作者时作为关键词")
    void fallback_关键词兜底() {
        var service = new DeepSeekService(restTemplate, "", "deepseek-chat", "https://api.deepseek.com", 0.1);

        var result = service.parseQuery("机器学习入门");

        assertEquals("机器学习入门", result.keyword());
        assertNull(result.category());
        assertNull(result.author());
    }

    @Test
    @DisplayName("降级解析：空字符串返回所有字段为 null")
    void fallback_空字符串() {
        var service = new DeepSeekService(restTemplate, "", "deepseek-chat", "https://api.deepseek.com", 0.1);

        var result = service.parseQuery("");

        assertAll(
                () -> assertNull(result.category()),
                () -> assertNull(result.keyword()),
                () -> assertNull(result.author())
        );
    }

    // ─── 降级解析器直接测试 ───

    @Nested
    @DisplayName("FallbackParser 直接测试")
    class FallbackParserDirectTest {

        private final DeepSeekService service =
                new DeepSeekService(null, "", "deepseek-chat", "https://api.deepseek.com", 0.1);

        @Test
        @DisplayName("分类和作者同时出现时两者都提取")
        void 分类和作者同时提取() {
            var result = service.fallbackParse("东野圭吾的推理小说");
            assertEquals("推理", result.category());
            assertEquals("东野圭吾", result.author());
        }

        @Test
        @DisplayName("作者优先于分类提取")
        void 作者优先() {
            var result = service.fallbackParse("刘慈欣的科幻");
            assertEquals("科幻", result.category());
            assertEquals("刘慈欣", result.author());
        }
    }

    private void mockApiResponse(String content) {
        var json = "{\"choices\":[{\"message\":{\"content\":\"%s\"}}]}"
                .formatted(content.replace("\"", "\\\"").replace("\n", "\\n"));
        when(restTemplate.postForObject(any(), any(), eq(String.class)))
                .thenReturn(json);
    }
}
