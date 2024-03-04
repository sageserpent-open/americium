package com.sageserpent.americium.java.junit5;

import com.sageserpent.americium.java.TrialsScaffolding;
import com.sageserpent.americium.junit5.package$;
import cyclops.data.tuple.Tuple2;
import cyclops.data.tuple.Tuple3;
import cyclops.data.tuple.Tuple4;
import cyclops.function.Consumer3;
import cyclops.function.Consumer4;
import org.junit.jupiter.api.DynamicTest;
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.FunctionConverters;

import java.util.Iterator;
import java.util.function.BiConsumer;
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
        return package$.MODULE$.dynamicTests(
                CollectionConverters.asScala(supplier.testIntegrationContexts()),
                FunctionConverters.asScalaFromConsumer(consumer));
    }

    /**
     * Overload for a parameterised test taking two arguments.
     * Consult the Javadoc above for
     * {@link JUnit5#dynamicTests(TrialsScaffolding.SupplyToSyntax, Consumer)}.
     */
    public static <Case1, Case2> Iterator<DynamicTest> dynamicTests(
            TrialsScaffolding.SupplyToSyntax<Tuple2<Case1, Case2>> supplier,
            BiConsumer<Case1, Case2> biConsumer) {
        return package$.MODULE$.dynamicTests(
                CollectionConverters.asScala(supplier.testIntegrationContexts()),
                FunctionConverters.asScalaFromConsumer(pair -> biConsumer.accept(
                        pair._1(),
                        pair._2())));
    }

    /**
     * Overload for a parameterised test taking three arguments.
     * Consult the Javadoc above for
     * {@link JUnit5#dynamicTests(TrialsScaffolding.SupplyToSyntax, Consumer)}.
     */
    public static <Case1, Case2, Case3> Iterator<DynamicTest> dynamicTests(
            TrialsScaffolding.SupplyToSyntax<Tuple3<Case1, Case2, Case3>> supplier,
            Consumer3<Case1, Case2, Case3> triConsumer) {
        return package$.MODULE$.dynamicTests(
                CollectionConverters.asScala(supplier.testIntegrationContexts()),
                FunctionConverters.asScalaFromConsumer(triple -> triConsumer.accept(
                        triple._1(),
                        triple._2(),
                        triple._3())));
    }

    /**
     * Overload for a parameterised test taking four arguments.
     * Consult the Javadoc above for
     * {@link JUnit5#dynamicTests(TrialsScaffolding.SupplyToSyntax, Consumer)}.
     */
    public static <Case1, Case2, Case3, Case4> Iterator<DynamicTest> dynamicTests(
            TrialsScaffolding.SupplyToSyntax<Tuple4<Case1, Case2, Case3,
                    Case4>> supplier,
            Consumer4<Case1, Case2, Case3, Case4> quadConsumer) {
        return package$.MODULE$.dynamicTests(
                CollectionConverters.asScala(supplier.testIntegrationContexts()),
                FunctionConverters.asScalaFromConsumer(quadruple -> quadConsumer.accept(
                        quadruple._1(),
                        quadruple._2(),
                        quadruple._3(),
                        quadruple._4())));
    }
}
