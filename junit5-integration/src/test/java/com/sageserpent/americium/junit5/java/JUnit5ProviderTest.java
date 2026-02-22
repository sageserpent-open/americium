package com.sageserpent.americium.junit5.java;

import com.google.common.collect.ImmutableSet;
import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsApi;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Iterator;
import java.util.Map;


class JUnit5ProviderTest {
    private final static TrialsApi api = Trials.api();

    static Iterator<ImmutableSet<String>> sets() {
        return JUnit5Provider.of(30,
                                 api.characters().strings().immutableSets());
    }

    static Iterator<Arguments> mixtures() {
        return JUnit5Provider.of(32,
                                 api.integers(),
                                 api
                                         .characters()
                                         .strings()
                                         .immutableMaps(api.booleans()));
    }

    @ParameterizedTest
    @MethodSource(value = "sets")
    void testDriveSetsProvider(ImmutableSet<String> distinctStrings) {
        System.out.println(distinctStrings);
    }

    @ParameterizedTest
    @MethodSource(value = "mixtures")
    void testDriveMixturesProvider(int integer,
                                   Map<String, Boolean> dictionary) {
        System.out.printf("%d, %s%n", integer, dictionary);
    }
}