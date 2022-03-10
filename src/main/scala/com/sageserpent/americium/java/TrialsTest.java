package com.sageserpent.americium.java;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestTemplate
@ExtendWith(TrialsTestExtension.class)
public @interface TrialsTest {
    String[] trials() default "";

    int casesLimit();

    int complexity() default 100;

    int shrinkageAttempts() default 100;
}
