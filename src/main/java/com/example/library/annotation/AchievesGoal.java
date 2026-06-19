package com.example.library.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 Action 成功执行即达成某一 Goal。
 * <p>
 * 当该 Action 正常返回时，Embabel 引擎判定对应 Goal 已达成，
 * OODA 循环可以终止。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AchievesGoal {
    String value();
}
