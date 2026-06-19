package com.example.library.action;

import com.embabel.agent.api.annotation.Action;
import com.example.library.domain.Book;
import com.example.library.condition.IsAvailableCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckAvailabilityAction {

    private static final Logger log = LoggerFactory.getLogger(CheckAvailabilityAction.class);

    @Autowired
    private IsAvailableCondition isAvailableCondition;

    /**
     * 过滤出可借的图书（逐本实时检查数据库状态）。
     */
    @Action(cost = 0, description = "逐本检查图书是否可借（实时读库）")
    public List<Book> execute(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        var available = books.stream()
                .filter(b -> isAvailableCondition.test(b))
                .toList();
        log.info("action=CheckAvailability cost=0 input={} available={}", books.size(), available.size());
        return available;
    }
}
