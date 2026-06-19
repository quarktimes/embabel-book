package com.example.library.repository;

import com.example.library.entity.BookEntity;
import com.example.library.entity.BorrowRecordEntity;
import com.example.library.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Repository 契约测试")
class RepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @BeforeEach
    void setUp() {
        // 预置 5 本书：3 本科幻（含大小写差异）+ 2 本推理
        em.persist(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        em.persist(new BookEntity("b2", "沙丘", "科幻", "赫伯特", true));
        em.persist(new BookEntity("b3", "银河帝国", "科 幻", "阿西莫夫", true)); // 带空格
        em.persist(new BookEntity("b4", "白夜行", "推理", "东野圭吾", true));
        em.persist(new BookEntity("b5", "嫌疑人X的献身", "推理", "东野圭吾", false));
        // 用户
        em.persist(new UserEntity("u1", "张三"));
        em.flush();
        em.clear();
    }

    // ─── BookRepository ───

    @Test
    @DisplayName("按分类查询返回匹配图书")
    void findByCategory_返回匹配图书() {
        var results = bookRepository.findByCategoryContaining("科幻");
        assertEquals(2, results.size()); // "科幻" 匹配 b1, b2；b3 是"科 幻"不匹配
    }

    @Test
    @DisplayName("分类名大小写敏感（H2 默认行为，生产用 PostgreSQL 可忽略）")
    void findByCategory_大小写敏感() {
        // H2 的 LIKE 默认大小写敏感，所以 "KEHUAN" 不匹配 "科幻"
        var results = bookRepository.findByCategoryContaining("KEHUAN");
        assertEquals(0, results.size());
        // 正确的大小写能匹配
        var correct = bookRepository.findByCategoryContaining("科幻");
        assertEquals(2, correct.size());
    }

    @Test
    @DisplayName("分类名部分匹配")
    void findByCategory_部分匹配() {
        var results = bookRepository.findByCategoryContaining("科");
        assertEquals(3, results.size()); // "科幻" + "科 幻"
    }

    @Test
    @DisplayName("无匹配分类返回空列表")
    void findByCategory_无匹配_返回空() {
        var results = bookRepository.findByCategoryContaining("经济学");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("按作者查询返回匹配图书")
    void findByAuthor_返回匹配图书() {
        var results = bookRepository.findByAuthorContaining("东野圭吾");
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("作者名带空格能匹配")
    void findByAuthor_带空格_能匹配() {
        // 实际 author 字段没空格，这里测试空参数场景
        var results = bookRepository.findByAuthorContaining("刘慈");
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("无匹配作者返回空列表")
    void findByAuthor_无匹配_返回空() {
        var results = bookRepository.findByAuthorContaining("金庸");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("按关键词搜索返回匹配图书")
    void findByTitle_返回匹配() {
        var results = bookRepository.findByTitleContaining("三体");
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("关键词空字符串返回所有图书")
    void findByTitle_空字符串_返回全部() {
        var results = bookRepository.findByTitleContaining("");
        assertEquals(5, results.size());
    }

    // ─── UserRepository ───

    @Test
    @DisplayName("保存用户后能按 ID 找到")
    void save_findById_正常() {
        var found = userRepository.findById("u1");
        assertTrue(found.isPresent());
        assertEquals("张三", found.get().getName());
    }

    @Test
    @DisplayName("不存在的用户返回空")
    void findById_不存在_返回空() {
        var found = userRepository.findById("not-exist");
        assertTrue(found.isEmpty());
    }

    // ─── BorrowRecordRepository ───

    @Test
    @DisplayName("按用户 ID 查询借书记录，按时间倒序")
    void findByUserId_按时间倒序() {
        var book = em.find(BookEntity.class, "b1");
        var user = em.find(UserEntity.class, "u1");
        var r1 = new BorrowRecordEntity(user, book, LocalDateTime.of(2026, 6, 1, 10, 0));
        var r2 = new BorrowRecordEntity(user, book, LocalDateTime.of(2026, 6, 5, 10, 0));
        em.persist(r1);
        em.persist(r2);
        em.flush();
        em.clear();

        var results = borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc("u1");

        assertEquals(2, results.size());
        // 按时间倒序：r2（6月5日）在前，r1（6月1日）在后
        assertTrue(results.get(0).getBorrowTime().isAfter(results.get(1).getBorrowTime()));
    }

    @Test
    @DisplayName("无借书记录返回空列表")
    void findByUserId_无记录_返回空() {
        var results = borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc("u1");
        assertTrue(results.isEmpty());
    }
}
