package com.example.library.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为 OODA 循环中的 Condition 守卫条件。
 * <p>
 * 返回 false 时阻断对应 Action 的执行流程。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {
    // 可指定关联的 Condition class（用于语法检查）
    Class<?> value() default Object.class;
}
