package com.example.library.entity;

import com.example.library.domain.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("BookEntity 契约测试")
class BookEntityTest {

    @Autowired
    private TestEntityManager em;

    // ─── toDomain 映射 ───

    @Test
    @DisplayName("toDomain 将所有字段正确映射到 Book record")
    void toDomain_字段映射正确() {
        var entity = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);

        var book = entity.toDomain();

        assertAll(
                () -> assertEquals("b1", book.id()),
                () -> assertEquals("三体", book.title()),
                () -> assertEquals("科幻", book.category()),
                () -> assertEquals("刘慈欣", book.author()),
                () -> assertTrue(book.available())
        );
    }

    @Test
    @DisplayName("available=false 时 toDomain 映射正确")
    void toDomain_不可借状态映射正确() {
        var entity = new BookEntity("b1", "三体", "科幻", "刘慈欣", false);

        var book = entity.toDomain();

        assertFalse(book.available());
    }

    // ─── fromDomain 映射 ───

    @Test
    @DisplayName("fromDomain 从 Book record 正确创建 Entity")
    void fromDomain_字段映射正确() {
        var book = new Book("b1", "三体", "科幻", "刘慈欣", true);

        var entity = BookEntity.fromDomain(book);

        assertAll(
                () -> assertEquals("b1", entity.getId()),
                () -> assertEquals("三体", entity.getTitle()),
                () -> assertEquals("科幻", entity.getCategory()),
                () -> assertEquals("刘慈欣", entity.getAuthor()),
                () -> assertTrue(entity.isAvailable())
        );
    }

    // ─── JPA 持久化 ───

    @Test
    @DisplayName("保存后从数据库读取，字段值完整不丢失")
    void 保存后读取_字段一致() {
        var entity = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);
        em.persistAndFlush(entity);
        em.clear();

        var found = em.find(BookEntity.class, "b1");

        assertAll(
                () -> assertNotNull(found),
                () -> assertEquals("三体", found.getTitle()),
                () -> assertEquals("科幻", found.getCategory()),
                () -> assertEquals("刘慈欣", found.getAuthor()),
                () -> assertTrue(found.isAvailable())
        );
    }

    @Test
    @DisplayName("toDomain 和 fromDomain 互为逆操作")
    void toDomain_fromDomain_互为逆操作() {
        var original = new BookEntity("b1", "三体", "科幻", "刘慈欣", true);

        var book = original.toDomain();
        var restored = BookEntity.fromDomain(book);

        assertAll(
                () -> assertEquals(original.getId(), restored.getId()),
                () -> assertEquals(original.getTitle(), restored.getTitle()),
                () -> assertEquals(original.getCategory(), restored.getCategory()),
                () -> assertEquals(original.getAuthor(), restored.getAuthor()),
                () -> assertEquals(original.isAvailable(), restored.isAvailable())
        );
    }

    // ─── 边界情况 ───

    @Test
    @DisplayName("分类名为空时能正常持久化")
    void 分类名为空_能保存() {
        var entity = new BookEntity("b2", "无分类书", null, "佚名", true);

        em.persistAndFlush(entity);
        em.clear();

        var found = em.find(BookEntity.class, "b2");
        assertNull(found.getCategory());
    }

    @Test
    @DisplayName("长书名（200 字内）能正常保存")
    void 长书名_能保存() {
        var longTitle = "长".repeat(200);
        var entity = new BookEntity("b3", longTitle, "测试", "测试", true);

        em.persistAndFlush(entity);
        em.clear();

        var found = em.find(BookEntity.class, "b3");
        assertEquals(200, found.getTitle().length());
    }

    @Test
    @DisplayName("available 字段默认为 true")
    void available_默认值_true() {
        var entity = new BookEntity();
        assertTrue(entity.isAvailable());
    }
}
