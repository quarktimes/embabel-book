package com.example.library.action;

import com.example.library.domain.Book;
import com.example.library.domain.User;
import com.example.library.entity.BookEntity;
import com.example.library.entity.UserEntity;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("ExecuteBorrowAction 契约测试")
class ExecuteBorrowActionTest {

    @Autowired
    private ExecuteBorrowAction action;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @BeforeEach
    void cleanUp() {
        borrowRecordRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("借书成功 → 图书状态变 false")
    void 借书成功_图书状态变false() {
        bookRepository.save(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        userRepository.save(new UserEntity("u1", "张三"));

        var book = new Book("b1", "三体", "科幻", "刘慈欣", true);
        var user = new User("u1", "张三", List.of());

        action.execute(book, user);

        var saved = bookRepository.findById("b1").orElseThrow();
        assertFalse(saved.isAvailable());
    }

    @Test
    @DisplayName("借书成功 → 新增一条借书记录")
    void 借书成功_新增借书记录() {
        bookRepository.save(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        userRepository.save(new UserEntity("u1", "张三"));

        action.execute(
                new Book("b1", "三体", "科幻", "刘慈欣", true),
                new User("u1", "张三", List.of()));

        var records = borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc("u1");
        assertEquals(1, records.size());
        assertEquals("b1", records.get(0).getBook().getId());
        assertNull(records.get(0).getReturnTime());
    }

    @Test
    @DisplayName("借书成功 → 返回成功信息")
    void 借书成功_返回成功信息() {
        bookRepository.save(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        userRepository.save(new UserEntity("u1", "张三"));

        var result = action.execute(
                new Book("b1", "三体", "科幻", "刘慈欣", true),
                new User("u1", "张三", List.of()));

        assertTrue(result.success());
        assertTrue(result.message().contains("三体"));
    }

    @Test
    @DisplayName("图书已被借走 → 抛异常")
    void 图书已被借走_抛异常() {
        bookRepository.save(new BookEntity("b1", "三体", "科幻", "刘慈欣", false)); // 不可借
        userRepository.save(new UserEntity("u1", "张三"));

        assertThrows(IllegalStateException.class, () ->
                action.execute(
                        new Book("b1", "三体", "科幻", "刘慈欣", false),
                        new User("u1", "张三", List.of())));
    }

    @Test
    @DisplayName("图书不存在 → 抛异常")
    void 图书不存在_抛异常() {
        userRepository.save(new UserEntity("u1", "张三"));

        assertThrows(IllegalArgumentException.class, () ->
                action.execute(
                        new Book("not-exist", "未知", null, null, true),
                        new User("u1", "张三", List.of())));
    }
}
