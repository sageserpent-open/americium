package com.sageserpent.americium.java.junit5;

import com.sageserpent.americium.java.TrialsScaffolding;
import com.sageserpent.americium.junit5.package$;
import org.junit.jupiter.api.DynamicTest;
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.FunctionConverters;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class JUnit5 {
    /**
     * Provide dynamic tests for JUnit5, allowing direct coupling between a
     * {@link TrialsScaffolding.SupplyToSyntax} and the parameterised test
     * consuming the supplied test cases.
     * <p>
     * In contrast to the 'classic' approach taken by {@link TrialsTest} and
     * {@link ConfiguredTrialsTest}, this affords strong type-checking
     * between the test's arguments and the supplier.
     * <p>
     * It does however sacrifice the potential use of
     * {@link org.junit.jupiter.api.BeforeEach} and
     * {@link org.junit.jupiter.api.AfterEach} to perform set-up and
     * tear-down for each trial - that has to be coded explicitly in the
     * parameterised test itself.
     * <p>
     * Example:
     * <pre>{@code
     *     // Annotation to wire the dynamic tests into JUnit5...
     *     @TestFactory
     *     Iterator<DynamicTest> dynamicTestsExample() {
     *         final TrialsScaffolding.SupplyToSyntax<Integer> supplier =
     *                 Trials.api().integers().withLimit(10);
     *
     *         return JUnit5.dynamicTests(
     *                 supplier,
     *                 // The parameterised test: it just prints out the test case...
     *                 testCase -> {
     *                     System.out.format("Test case %d\n", testCase);
     *                 });
     *     }}</pre>
     *
     * @param supplier Supply syntax instance created by
     *                 {@link TrialsScaffolding#withLimit(int)} or
     *                 {@link TrialsScaffolding#withStrategy(Function)}.
     * @param consumer Parameterised test that consumes a test case of type
     *                 {@code Case}.
     * @return An iterator of {@link DynamicTest} instances, suitable for use
     * with the {@link org.junit.jupiter.api.TestFactory} annotation provided
     * by JUnit5.
     */
    public static <Case> Iterator<DynamicTest> dynamicTests(
            TrialsScaffolding.SupplyToSyntax<Case> supplier,
            Consumer<Case> consumer) {
        return CollectionConverters.asJava(package$.MODULE$.dynamicTests(
                CollectionConverters.asScala(supplier.testIntegrationContexts()),
                FunctionConverters.asScalaFromConsumer(consumer)));
    }
}
