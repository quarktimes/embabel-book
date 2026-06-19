package com.example.library.action;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import com.example.library.domain.BorrowRequest;
import com.example.library.domain.ParsedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParseQueryAction {

    private static final Logger log = LoggerFactory.getLogger(ParseQueryAction.class);

    @Autowired
    private Ai ai;

    @Action(cost = 5, description = "使用 LLM 解析自然语言查询为结构化查询条件")
    public ParsedQuery execute(BorrowRequest request) {
        log.info("action=ParseQuery cost=5 request={}", request.query());

        var llm = LlmOptions.withModel("deepseek-chat")
                .withTemperature(0.1);

        return ai.withLlm(llm)
                .withSystemPrompt("""
                        你是一个图书查询解析助手。将用户的自然语言查询解析为结构化信息。
                        返回 JSON 格式，不要包含 Markdown 代码块标记。

                        字段说明：
                        - category: 图书分类（科幻、推理、计算机、文学、历史...），没有则为 null
                        - keyword: 关键词，没有则为 null
                        - author: 作者名，没有则为 null

                        示例：
                        用户：我想借一些科幻小说
                        返回：{"category":"科幻","keyword":null,"author":null}

                        用户：有没有东野圭吾的推理小说
                        返回：{"category":"推理","keyword":null,"author":"东野圭吾"}

                        用户：推荐机器学习的书
                        返回：{"category":"计算机","keyword":"机器学习","author":null}
                        """)
                .creating(ParsedQuery.class)
                .fromPrompt(request.query());
    }
}
