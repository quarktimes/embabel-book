package com.example.library.condition;

import com.embabel.agent.api.annotation.Condition;
import com.example.library.domain.BorrowRequest;
import org.springframework.stereotype.Component;

@Component
public class HasValidQueryCondition {

    @Condition(name = "HasValidQuery", cost = 0)
    public boolean test(BorrowRequest request) {
        if (request == null || request.query() == null) {
            return false;
        }
        return !request.query().isBlank();
    }
}
