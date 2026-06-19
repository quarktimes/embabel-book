package com.example.library.action;

import com.example.library.domain.Book;
import com.example.library.domain.BorrowResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReturnResultAction 契约测试")
class ReturnResultActionTest {

    private final ReturnResultAction action = new ReturnResultAction();

    @Test
    @DisplayName("成功时返回信息")
    void 成功时返回信息() {
        var book = new Book("b1", "三体", "科幻", "刘慈欣", true);
        var result = new BorrowResult(true, "成功借阅《三体》", book);

        var output = action.execute(result);

        assertEquals("成功借阅《三体》", output);
    }

    @Test
    @DisplayName("失败时返回默认提示")
    void 失败时返回默认提示() {
        var book = new Book("b1", "三体", "科幻", "刘慈欣", false);
        var result = new BorrowResult(false, "失败", book);

        var output = action.execute(result);

        assertEquals("抱歉，暂时无法借阅", output);
    }

    @Test
    @DisplayName("null 结果 → 返回系统异常提示")
    void null结果() {
        assertEquals("抱歉，系统异常，请稍后重试", action.execute(null));
    }
}
