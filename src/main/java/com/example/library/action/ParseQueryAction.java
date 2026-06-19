package com.example.library.action;

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

    public ParsedQuery execute(BorrowRequest request) {
        log.info("action=ParseQuery cost=5 request={}", request.query());
        return deepSeekService.parseQuery(request.query());
    }
}
