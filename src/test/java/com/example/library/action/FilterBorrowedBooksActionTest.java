package com.example.library.action;

import com.example.library.domain.Book;
import com.example.library.domain.User;
import com.example.library.condition.NotBorrowedBeforeCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilterBorrowedBooksAction 契约测试")
class FilterBorrowedBooksActionTest {

    @Mock
    private NotBorrowedBeforeCondition condition;

    @InjectMocks
    private FilterBorrowedBooksAction action;

    private final User user = new User("u1", "张三", List.of("b1"));
    private final Book book1 = new Book("b1", "三体", "科幻", "刘慈欣", true);
    private final Book book2 = new Book("b2", "沙丘", "科幻", "赫伯特", true);

    @Test
    @DisplayName("过滤已借，保留未借")
    void 过滤已借() {
        when(condition.test(user, book1)).thenReturn(false);
        when(condition.test(user, book2)).thenReturn(true);

        var result = action.execute(List.of(book1, book2), user);

        assertEquals(1, result.size());
        assertEquals("沙丘", result.get(0).title());
    }

    @Test
    @DisplayName("全部已借过 → 返回空")
    void 全部已借() {
        when(condition.test(any(), any())).thenReturn(false);

        var result = action.execute(List.of(book1, book2), user);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("全部未借过 → 返回所有")
    void 全部未借() {
        when(condition.test(any(), any())).thenReturn(true);

        var result = action.execute(List.of(book1, book2), user);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("部分已借 → 只返回未借的")
    void 部分已借() {
        when(condition.test(user, book1)).thenReturn(false);
        when(condition.test(user, book2)).thenReturn(true);

        var result = action.execute(List.of(book1, book2), user);

        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(b -> b.id().equals("b1")));
    }

    @Test
    @DisplayName("空输入 → 返回空")
    void 空输入() {
        assertTrue(action.execute(List.of(), user).isEmpty());
    }
}
