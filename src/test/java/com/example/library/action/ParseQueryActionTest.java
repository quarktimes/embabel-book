package com.example.library.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("ParseQueryAction 契约测试")
class ParseQueryActionTest {

    @Autowired
    private ParseQueryAction action;

    @Test
    @DisplayName("ParseQueryAction 注入正常")
    void action注入正常() {
        assertNotNull(action);
    }
}
