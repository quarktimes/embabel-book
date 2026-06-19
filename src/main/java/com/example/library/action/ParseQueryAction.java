package com.example.library.action;

import com.embabel.agent.api.annotation.Action;
import com.example.library.domain.BorrowRequest;
import com.example.library.domain.ParsedQuery;
import com.example.library.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParseQueryAction {

    private static final Logger log = LoggerFactory.getLogger(ParseQueryAction.class);

    @Autowired
    private DeepSeekService deepSeekService;

    @Action(cost = 5, description = "使用 DeepSeek LLM 解析自然语言查询为结构化查询条件")
    public ParsedQuery execute(BorrowRequest request) {
        log.info("action=ParseQuery cost=5 request={}", request.query());
        return deepSeekService.parseQuery(request.query());
    }
}
