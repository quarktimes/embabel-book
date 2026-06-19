package com.example.library.action;

import com.example.library.domain.BorrowResult;
import org.springframework.stereotype.Component;

@Component
public class ReturnResultAction {

    /**
     * 格式化借书结果为字符串。
     * 成功时返回具体信息，失败时返回通用提示。
     */
    public String execute(BorrowResult result) {
        if (result == null) {
            return "抱歉，系统异常，请稍后重试";
        }
        return result.success()
                ? result.message()
                : "抱歉，暂时无法借阅";
    }
}
