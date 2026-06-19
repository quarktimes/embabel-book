package com.example.library.action;

import com.example.library.domain.BorrowRequest;
import com.example.library.domain.ParsedQuery;
import com.example.library.service.DeepSeekService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParseQueryAction 契约测试")
class ParseQueryActionTest {

    @Mock
    private DeepSeekService deepSeekService;

    @InjectMocks
    private ParseQueryAction action;

    @Test
    @DisplayName("有效请求 → 调 DeepSeek 返回解析结果")
    void 有效请求_返回解析结果() {
        when(deepSeekService.parseQuery("科幻小说"))
                .thenReturn(new ParsedQuery("科幻", null, null));

        var result = action.execute(new BorrowRequest("u1", "科幻小说"));

        assertEquals("科幻", result.category());
        verify(deepSeekService).parseQuery("科幻小说");
    }
}
