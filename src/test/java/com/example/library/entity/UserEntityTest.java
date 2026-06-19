package com.example.library.entity;

import com.example.library.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("UserEntity + BorrowRecordEntity 契约测试")
class UserEntityTest {

    @Autowired
    private TestEntityManager em;

    // ─── UserEntity toDomain ───

    @Test
    @DisplayName("无借书记录时 toDomain 返回空列表")
    void toDomain_无借书记录_返回空列表() {
        var user = new UserEntity("u1", "张三");
        em.persistAndFlush(user);
        em.clear();

        var found = em.find(UserEntity.class, "u1");
        var domain = found.toDomain();

        assertAll(
                () -> assertEquals("u1", domain.id()),
                () -> assertEquals("张三", domain.name()),
                () -> assertTrue(domain.borrowedBookIds().isEmpty())
        );
    }

    @Test
    @DisplayName("有未归还图书时 toDomain 包含其 ID")
    void toDomain_有未归还图书_包含ID() {
        var user = new UserEntity("u1", "张三");
        var book = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);
        em.persist(user);
        em.persist(book);
        var record = new BorrowRecordEntity(user, book, LocalDateTime.now());
        em.persist(record);
        em.flush();
        em.clear();

        var found = em.find(UserEntity.class, "u1");
        var domain = found.toDomain();

        assertEquals(1, domain.borrowedBookIds().size());
        assertEquals("b1", domain.borrowedBookIds().getFirst());
    }

    @Test
    @DisplayName("已归还的图书不包含在 borrowedBookIds 中")
    void toDomain_已归还_不包含() {
        var user = new UserEntity("u1", "张三");
        var book = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);
        em.persist(user);
        em.persist(book);
        var record = new BorrowRecordEntity(user, book, LocalDateTime.now());
        record.setReturnTime(LocalDateTime.now()); // 已归还
        em.persist(record);
        em.flush();
        em.clear();

        var found = em.find(UserEntity.class, "u1");
        var domain = found.toDomain();

        assertTrue(domain.borrowedBookIds().isEmpty());
    }

    @Test
    @DisplayName("多本未归还图书全部包含")
    void toDomain_多本未归还_全部包含() {
        var user = new UserEntity("u1", "张三");
        var book1 = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);
        var book2 = new BookEntity("b2", "沙丘", "科幻", "赫伯特", true);
        em.persist(user);
        em.persist(book1);
        em.persist(book2);
        em.persist(new BorrowRecordEntity(user, book1, LocalDateTime.now()));
        em.persist(new BorrowRecordEntity(user, book2, LocalDateTime.now()));
        em.flush();
        em.clear();

        var domain = em.find(UserEntity.class, "u1").toDomain();

        assertEquals(2, domain.borrowedBookIds().size());
        assertTrue(domain.borrowedBookIds().containsAll(List.of("b1", "b2")));
    }

    // ─── BorrowRecordEntity 关联 ───

    @Test
    @DisplayName("BorrowRecord 正确关联 User 和 Book")
    void borrowRecord_关联正确() {
        var user = new UserEntity("u1", "张三");
        var book = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);
        em.persist(user);
        em.persist(book);
        var record = new BorrowRecordEntity(user, book, LocalDateTime.of(2026, 6, 1, 10, 0));
        em.persistAndFlush(record);
        em.clear();

        var found = em.find(BorrowRecordEntity.class, record.getId());

        assertAll(
                () -> assertNotNull(found),
                () -> assertEquals("u1", found.getUser().getId()),
                () -> assertEquals("b1", found.getBook().getId()),
                () -> assertEquals(LocalDateTime.of(2026, 6, 1, 10, 0), found.getBorrowTime()),
                () -> assertNull(found.getReturnTime())
        );
    }

    @Test
    @DisplayName("BorrowRecord 设置归还时间后能正确读取")
    void borrowRecord_归还时间() {
        var user = new UserEntity("u1", "张三");
        var book = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);
        em.persist(user);
        em.persist(book);
        var record = new BorrowRecordEntity(user, book, LocalDateTime.now());
        record.setReturnTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        em.persistAndFlush(record);
        em.clear();

        var found = em.find(BorrowRecordEntity.class, record.getId());

        assertEquals(LocalDateTime.of(2026, 7, 1, 10, 0), found.getReturnTime());
    }
}
