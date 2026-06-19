package com.example.library.condition;

import com.example.library.domain.Book;
import com.example.library.entity.BookEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(IsAvailableCondition.class)
@DisplayName("IsAvailableCondition 契约测试")
class IsAvailableConditionTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private IsAvailableCondition condition;

    @Test
    @DisplayName("图书可借 → true（存在且 available=true）")
    void available_true() {
        em.persistAndFlush(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        em.clear();

        assertTrue(condition.test(new Book("b1", "三体", "科幻", "刘慈欣", true)));
    }

    @Test
    @DisplayName("图书不可借 → false（available=false）")
    void available_false() {
        em.persistAndFlush(new BookEntity("b1", "三体", "科幻", "刘慈欣", false));
        em.clear();

        assertFalse(condition.test(new Book("b1", "三体", "科幻", "刘慈欣", false)));
    }

    @Test
    @DisplayName("图书不存在 → false")
    void available_notFound() {
        assertFalse(condition.test(new Book("not-exist", "未知", null, null, true)));
    }

    @Test
    @DisplayName("两次调用结果可不同 — 状态变更后重新读取")
    void 动态性_状态变更后重新读取() {
        em.persistAndFlush(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        em.clear();

        var book = new Book("b1", "三体", "科幻", "刘慈欣", true);

        // 第一次调用：可借
        assertTrue(condition.test(book));

        // 数据库状态变更（另一个线程/请求修改了）
        var entity = em.find(BookEntity.class, "b1");
        entity.setAvailable(false);
        em.flush();
        em.clear();

        // 第二次调用：不可借（不依赖传入的 book.available，重新读库）
        assertFalse(condition.test(book));
    }
}
