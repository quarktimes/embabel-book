package com.example.library.action;

import com.example.library.domain.Book;
import com.example.library.condition.IsAvailableCondition;
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
@DisplayName("CheckAvailabilityAction 契约测试")
class CheckAvailabilityActionTest {

    @Mock
    private IsAvailableCondition isAvailableCondition;

    @InjectMocks
    private CheckAvailabilityAction action;

    private final Book book1 = new Book("b1", "三体", "科幻", "刘慈欣", true);
    private final Book book2 = new Book("b2", "沙丘", "科幻", "赫伯特", false);

    @Test
    @DisplayName("过滤不可借，保留可借")
    void 过滤不可借() {
        when(isAvailableCondition.test(book1)).thenReturn(true);
        when(isAvailableCondition.test(book2)).thenReturn(false);

        var result = action.execute(List.of(book1, book2));

        assertEquals(1, result.size());
        assertEquals("三体", result.get(0).title());
    }

    @Test
    @DisplayName("全部不可借 → 返回空")
    void 全部不可借() {
        when(isAvailableCondition.test(any())).thenReturn(false);

        var result = action.execute(List.of(book1, book2));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("全部可借 → 返回所有")
    void 全部可借() {
        when(isAvailableCondition.test(any())).thenReturn(true);

        var result = action.execute(List.of(book1, book2));

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("空输入 → 返回空")
    void 空输入() {
        var result = action.execute(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("null 输入 → 返回空")
    void null输入() {
        var result = action.execute(null);
        assertTrue(result.isEmpty());
    }
}
