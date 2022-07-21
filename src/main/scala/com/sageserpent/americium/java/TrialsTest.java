package com.sageserpent.americium.java;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Method annotation for use by parameterised test methods run via the JUnit5
 * engine extended with {@link TrialsTestExtension}. It couples one or
 * several instances of {@link Trials} to the test method, causing the method
 * to be run repeatedly with test cases realized as arguments supplied by the
 * {@link Trials} instance.
 * <p>
 * Once an invocation of the test method fails, the extension will go into a
 * shrinkage mode, invoking the test method repeatedly to find a combination
 * of arguments that comprise a simpler test case.
 * <p>
 * The usage is analogous to
 * {@link org.junit.jupiter.params.ParameterizedTest}. Like that annotation,
 * the test cases are directly repeatable, for example by IntelliJ's support
 * for running individual test cases - but only up to and including the first
 * failed test invocation. Subsequent test cases resulting from shrinkage
 * require a full run over invocations to be reproduced - except for the
 * minimised failing test case, as this can be reproduced via its recipe
 * hash, consult the test console output for instructions on how to do this.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestTemplate
@ExtendWith(TrialsTestExtension.class)
public @interface TrialsTest {
    /**
     * The name of a field or multiple fields of type {@link Trials} declared
     * in the class housing the annotated test method; the instances
     * referenced by the fields will collectively supply cases to the
     * annotated test. These fields should be static if the annotated class
     * uses {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_METHOD},
     * and non-static if
     * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS} is used.
     */
    String[] trials() default "";

    /**
     * @see Trials#withLimits(int, Trials.OptionalLimits)
     */
    int casesLimit();

    /**
     * @see Trials.OptionalLimits#complexity
     */
    int complexity() default TrialsDefaults.defaultComplexityLimit;

    /**
     * @see Trials.OptionalLimits#shrinkageAttempts
     */
    int shrinkageAttempts() default TrialsDefaults.defaultShrinkageAttemptsLimit;
}
