package com.example.library.action;

import com.example.library.domain.Book;
import com.example.library.domain.ParsedQuery;
import com.example.library.entity.BookEntity;
import com.example.library.repository.BookRepository;
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
@DisplayName("SearchBooksAction 契约测试")
class SearchBooksActionTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private SearchBooksAction action;

    @Test
    @DisplayName("按分类搜索 → 返回匹配图书")
    void 按分类搜索() {
        when(bookRepository.findByCategoryContaining("科幻"))
                .thenReturn(List.of(
                        new BookEntity("b1", "三体", "科幻", "刘慈欣", true),
                        new BookEntity("b2", "沙丘", "科幻", "赫伯特", true)
                ));

        var result = action.execute(new ParsedQuery("科幻", null, null));

        assertEquals(2, result.size());
        verify(bookRepository).findByCategoryContaining("科幻");
    }

    @Test
    @DisplayName("按作者搜索 → 返回匹配图书")
    void 按作者搜索() {
        when(bookRepository.findByAuthorContaining("东野圭吾"))
                .thenReturn(List.of(
                        new BookEntity("b3", "白夜行", "推理", "东野圭吾", true)
                ));

        var result = action.execute(new ParsedQuery(null, null, "东野圭吾"));

        assertEquals(1, result.size());
        assertEquals("白夜行", result.get(0).title());
    }

    @Test
    @DisplayName("按关键词搜索 → 返回匹配图书")
    void 按关键词搜索() {
        when(bookRepository.findByTitleContaining("机器学习"))
                .thenReturn(List.of(
                        new BookEntity("b4", "机器学习实战", "计算机", "Harrington", true)
                ));

        var result = action.execute(new ParsedQuery(null, "机器学习", null));

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("分类优先于其他参数")
    void 分类优先() {
        when(bookRepository.findByCategoryContaining("科幻"))
                .thenReturn(List.of(new BookEntity("b1", "三体", "科幻", "刘慈欣", true)));

        // 三个参数都有，但只用 category
        var result = action.execute(new ParsedQuery("科幻", "机器学习", "东野圭吾"));

        assertEquals(1, result.size());
        verify(bookRepository, never()).findByAuthorContaining(any());
        verify(bookRepository, never()).findByTitleContaining(any());
    }

    @Test
    @DisplayName("三个参数都 null → 返回空列表")
    void 三个参数都null_返回空() {
        var result = action.execute(new ParsedQuery(null, null, null));
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("无匹配分类 → 返回空列表")
    void 无匹配_返回空() {
        when(bookRepository.findByCategoryContaining("经济学"))
                .thenReturn(List.of());

        var result = action.execute(new ParsedQuery("经济学", null, null));
        assertTrue(result.isEmpty());
    }
}
