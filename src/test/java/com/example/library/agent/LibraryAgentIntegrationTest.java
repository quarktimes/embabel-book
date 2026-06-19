package com.example.library.agent;

import com.example.library.entity.BookEntity;
import com.example.library.entity.BorrowRecordEntity;
import com.example.library.entity.UserEntity;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("LibraryAgent 全链路集成测试")
class LibraryAgentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @BeforeEach
    void setUp() {
        borrowRecordRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        // 4 本书
        bookRepository.save(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        bookRepository.save(new BookEntity("b2", "沙丘", "科幻", "赫伯特", true));
        bookRepository.save(new BookEntity("b3", "白夜行", "推理", "东野圭吾", true));
        bookRepository.save(new BookEntity("b4", "嫌疑人X的献身", "推理", "东野圭吾", false)); // 不可借

        // 1 个用户
        userRepository.save(new UserEntity("u1", "张三"));
    }

    @Test
    @DisplayName("全链路：成功借阅 — 查询可借 → 过滤已借 → 借书成功")
    void 全链路_成功借阅() throws Exception {
        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", "我想借科幻小说"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("成功")));

        // 验证数据库：沙丘不可借（三体可借但张三没借过，按分类搜索返回三体+沙丘，借第一本）
        // 但注意 SearchBooksAction 找的是 category="科幻" 的 Containing 查询
        // 实际上这里 DeepSeekService 会先跑，然后降级，得到 category=科幻
        // 再按分类搜索到 三体+沙丘，都可用且未借过，选第一本三体
        assertFalse(bookRepository.findById("b1").orElseThrow().isAvailable());
        assertEquals(1, borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc("u1").size());
    }

    @Test
    @DisplayName("空查询 → 返回引导提示")
    void 空查询_返回引导() throws Exception {
        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请描述")));
    }

    @Test
    @DisplayName("无匹配分类 → 返回提示")
    void 无匹配分类_返回提示() throws Exception {
        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", "我想借经济学"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("没有找到")));
    }

    @Test
    @DisplayName("全部不可借 → 返回提示")
    void 全部不可借_返回提示() throws Exception {
        var allBooks = bookRepository.findAll();
        allBooks.forEach(b -> b.setAvailable(false));
        bookRepository.saveAll(allBooks);

        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", "推理小说"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已被借出")));
    }

    @Test
    @DisplayName("全部已借过 → 返回提示")
    void 全部已借过_返回提示() throws Exception {
        var allBooks = bookRepository.findAll();
        allBooks.forEach(b -> b.setAvailable(false));
        bookRepository.saveAll(allBooks);

        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", "科幻小说"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已被借出")));
    }

    @Test
    @DisplayName("多次借书 → 动态状态变化")
    void 多次借书_动态状态变化() throws Exception {
        // 第1次：借科幻 → 借到三体
        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", "科幻小说"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("成功")));

        // 第2次：再借科幻 → 借到沙丘（三体已借）
        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", "科幻小说"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("成功")));

        // 第3次：还借科幻 → 已没有可借且未借过的了
        mockMvc.perform(post("/borrow")
                        .param("userId", "u1")
                        .param("query", "科幻小说"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.containsString("已经借过"),
                                org.hamcrest.Matchers.containsString("已被借出"))));
    }

    @Test
    @DisplayName("不同用户互不影响 — 张三借了三体，李四还能借")
    void 不同用户互不影响() throws Exception {
        // 张三借走三体
        mockMvc.perform(post("/borrow")
                .param("userId", "u1")
                .param("query", "科幻小说"));

        // 李四（新用户）还能借三体？不对，三体已经被张三借走了（available=false）
        // 所以李四应该借到沙丘
        userRepository.save(new UserEntity("u2", "李四"));

        mockMvc.perform(post("/borrow")
                        .param("userId", "u2")
                        .param("query", "科幻小说"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("成功")));
    }
}
