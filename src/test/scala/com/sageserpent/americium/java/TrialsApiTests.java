package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

public class TrialsApiTests {
    private final static TrialsApi api = Trials.api();

    Trials<String> chainedIntegers() {
        return api.alternate(
                api.integers()
                        .flatMap(value -> chainedIntegers()
                                .map(chain -> String.join(",", value.toString(), chain))),
                api.only("*** END ***"));
    }

    Trials<String> chainedBooleansAndIntegersInATree() {
        return api.alternate(
                api.delay(() -> chainedBooleansAndIntegersInATree())
                        .flatMap(left -> api.booleans()
                                .flatMap(side -> chainedBooleansAndIntegersInATree()
                                        .map(right -> "(" + String.join(",", left, side.toString(), right) + ")"))),
                api.integers().map(value -> value.toString()));
    }

    Trials<String> chainedIntegersUsingAnExplicitTerminationCase() {
        return api.booleans().flatMap(terminate ->
                terminate ? api.only("*** END ***") : api.integers().flatMap(value -> chainedIntegersUsingAnExplicitTerminationCase().map(simplerCase -> String.join(",", value.toString(), simplerCase))));
    }

    @Test
    void testDrivePureJavaApi() {
        final Trials<Integer> integerTrials = api.only(1);
        final Trials<Double> doubleTrials =
                api.choose(new Double[]{1.2, 5.6, 0.1 + 0.1 + 0.1})
                        .map(value -> 10 * value);
        final BigDecimal oneTenthAsABigDecimal = BigDecimal.ONE.divide(BigDecimal.TEN);
        final Trials<BigDecimal> bigDecimalTrials =
                api.only(oneTenthAsABigDecimal.add(oneTenthAsABigDecimal).add(oneTenthAsABigDecimal))
                        .map(value -> value.multiply(BigDecimal.TEN));

        final Trials<? extends Number> alternateTrials = api.alternate(integerTrials, doubleTrials, bigDecimalTrials);

        final int limit = 20;

        alternateTrials.withLimit(limit).supplyTo(number -> {
            System.out.println(number.doubleValue());
        });

        final Trials<? extends Number> alternateTrialsFromArray = api.alternate(new Trials[]{integerTrials, doubleTrials, bigDecimalTrials});

        alternateTrialsFromArray.withLimit(limit).supplyTo(number -> {
            System.out.println(number.doubleValue());
        });

        System.out.println("Chained integers...");

        chainedIntegers().withLimit(limit).supplyTo(System.out::println);

        System.out.println("Chained integers using an explicit termination case...");

        chainedIntegersUsingAnExplicitTerminationCase().withLimit(limit).supplyTo(System.out::println);

        System.out.println("Chained integers and Booleans in a tree...");

        chainedBooleansAndIntegersInATree().withLimit(limit).supplyTo(System.out::println);

        System.out.println("A list of doubles...");

        doubleTrials.lists().withLimit(limit).supplyTo(System.out::println);

        System.out.println("A set of doubles...");

        doubleTrials.sets().withLimit(limit).supplyTo(System.out::println);

        System.out.println("A sorted set of doubles...");

        doubleTrials.sortedSets(Double::compareTo).withLimit(limit).supplyTo(System.out::println);

        System.out.println("A map of strings keyed by integers...");

        Trials<Integer> integersTrialsWithVariety = api.choose(1, 2, 3);

        integersTrialsWithVariety.maps(api.strings()).withLimit(limit).supplyTo(System.out::println);

        System.out.println("A sorted map of strings keyed by integers...");

        integersTrialsWithVariety.sortedMaps(Integer::compare, api.strings()).withLimit(limit).supplyTo(System.out::println);
    }

    static Iterator<? extends ImmutableSet<? extends String>> sets() {
        return JUnit5Provider.of(30, api.strings().sets());
    }

    @ParameterizedTest
    @MethodSource(value = "sets")
    void testDriveSetsProvider(ImmutableSet<? extends String> distinctStrings) {
        System.out.println(distinctStrings);
    }

    static Iterator<? extends Arguments> mixtures() {
        return JUnit5Provider.of(32, api.integers(), api.strings().maps(api.booleans()));
    }

    @ParameterizedTest
    @MethodSource(value = "mixtures")
    void testDriveMixturesProvider(int integer, Map<String, Boolean> dictionary) {
        System.out.println(String.format("%d, %s", integer, dictionary));
    }
}
