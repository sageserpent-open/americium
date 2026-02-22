package com.sageserpent.americium.junit5.java;

import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsScaffolding;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Method annotation for use by parameterised test methods run via the JUnit5
 * engine extended with {@link TrialsTestExtension}. It couples an instance
 * of {@link TrialsScaffolding.SupplyToSyntax} to the test method, causing
 * the method to
 * be run repeatedly with test cases realized as arguments supplied by the
 * {@link TrialsScaffolding.SupplyToSyntax} instance.
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
 * <p>
 * In contrast with {@link TrialsTest}, this annotation works with just one
 * field that can bring together multiple {@link Trials} instances and that
 * also implies the limit or limit strategy configuration - there is no
 * inline configuration in the annotation usage.
 *
 * @see TrialsTest
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestTemplate
@ExtendWith(TrialsTestExtension.class)
public @interface ConfiguredTrialsTest {
    /**
     * The name of a field of type {@link Trials.SupplyToSyntax} declared in
     * the class housing the annotated test method; the instance referenced
     * by the field will supply cases to the annotated test. This field
     * should be either static if the annotated class uses
     * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_METHOD}, or
     * non-static if
     * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS} is used.
     */
    String value() default "";
}
