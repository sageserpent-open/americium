package com.sageserpent.americium.java;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class TrialsApiTests {
    private final static TrialsApi api = Trials.api();

    Trials<String> chainedIntegers() {
        return api.alternate(
                api.integers()
                        .flatMap(value -> chainedIntegers()
                                .map(chain -> String.join(",", value.toString(), chain))),
                api.only(""));
    }

    Trials<String> chainedBooleansAndIntegersInATree() {
        return api.alternate(
                api.delay(() -> chainedBooleansAndIntegersInATree())
                        .flatMap(left -> api.coinFlip()
                                .flatMap(side -> chainedBooleansAndIntegersInATree()
                                        .map(right -> "(" + String.join(",", left, side.toString(), right) + ")"))),
                api.integers().map(value -> value.toString()));
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

        final Trials<? extends Number> alternateTrailsFromArray = api.alternate(new Trials[]{integerTrials, doubleTrials, bigDecimalTrials});

        alternateTrailsFromArray.withLimit(limit).supplyTo(number -> {
            System.out.println(number.doubleValue());
        });

        chainedIntegers().withLimit(limit).supplyTo(System.out::println);

        chainedBooleansAndIntegersInATree().withLimit(limit).supplyTo(System.out::println);
    }
}
