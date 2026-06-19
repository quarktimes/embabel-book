package com.example.library.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为 Embabel OODA 循环中的 Action。
 * <p>
 * cost 表示该 Action 的资源消耗强度：
 * - 5 = LLM 调用（最昂贵）
 * - 2 = 数据库查询
 * - 1 = 状态写入
 * - 0 = 纯内存操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
    int cost() default 0;
}
