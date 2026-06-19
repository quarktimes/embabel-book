package com.example.library.condition;

import com.example.library.domain.BorrowRequest;
import org.springframework.stereotype.Component;

@Component
public class HasValidQueryCondition {

    public boolean test(BorrowRequest request) {
        if (request == null || request.query() == null) {
            return false;
        }
        return !request.query().isBlank();
    }
}
