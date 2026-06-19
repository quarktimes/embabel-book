package com.example.library.condition;

import com.example.library.domain.Book;
import com.example.library.domain.BorrowRequest;
import com.example.library.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Condition 契约测试")
class ConditionTest {

    @Nested
    @DisplayName("HasValidQueryCondition")
    class HasValidQueryTest {

        private final HasValidQueryCondition condition = new HasValidQueryCondition();

        @Test @DisplayName("null 查询 → false")
        void nullQuery() {
            assertFalse(condition.test(new BorrowRequest("u1", null)));
        }

        @Test @DisplayName("空字符串 → false")
        void emptyQuery() {
            assertFalse(condition.test(new BorrowRequest("u1", "")));
        }

        @Test @DisplayName("纯空格 → false")
        void blankQuery() {
            assertFalse(condition.test(new BorrowRequest("u1", "   ")));
        }

        @Test @DisplayName("有效查询 → true")
        void validQuery() {
            assertTrue(condition.test(new BorrowRequest("u1", "科幻小说")));
        }
    }

    @Nested
    @DisplayName("NotBorrowedBeforeCondition")
    class NotBorrowedBeforeTest {

        private final NotBorrowedBeforeCondition condition = new NotBorrowedBeforeCondition();

        @Test @DisplayName("已借过该书 → false")
        void alreadyBorrowed() {
            var user = new User("u1", "张三", List.of("b1"));
            var book = new Book("b1", "三体", "科幻", "刘慈欣", true);
            assertFalse(condition.test(user, book));
        }

        @Test @DisplayName("未借过该书 → true")
        void notBorrowed() {
            var user = new User("u1", "张三", List.of("b1"));
            var book = new Book("b2", "沙丘", "科幻", "赫伯特", true);
            assertTrue(condition.test(user, book));
        }

        @Test @DisplayName("借阅列表为空 → true")
        void emptyList() {
            var user = new User("u1", "张三", List.of());
            var book = new Book("b1", "三体", "科幻", "刘慈欣", true);
            assertTrue(condition.test(user, book));
        }

        @Test @DisplayName("借阅列表为 null → true")
        void nullList() {
            var user = new User("u1", "张三", null);
            var book = new Book("b1", "三体", "科幻", "刘慈欣", true);
            assertTrue(condition.test(user, book));
        }
    }
}
