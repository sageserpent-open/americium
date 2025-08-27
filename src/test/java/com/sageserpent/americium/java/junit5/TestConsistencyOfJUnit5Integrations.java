package com.sageserpent.americium.java.junit5;

import com.google.common.collect.ImmutableList;
import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsFactoring;
import com.sageserpent.americium.java.examples.junit5.Tiers;
import cyclops.data.tuple.Tuple;
import cyclops.data.tuple.Tuple2;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.*;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.EventType;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sageserpent.americium.java.Trials.api;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

class TestConsistencyOfJUnit5Integrations {
    private final static ImmutableList<Tuple2<ImmutableList<Integer>,
            ImmutableList<Integer>>>
            canonicalTestCases;

    private final static Optional<Tuple2<ImmutableList<Integer>,
            ImmutableList<Integer>>>
            canonicalMaximallyShrunkTestCase;

    private static void sharedTestLogic(
            final ImmutableList<Integer> queryValues,
            final ImmutableList<Integer> feedSequence) {
        System.out.format("Query values: %s, feed sequence: %s\n",
                          queryValues,
                          feedSequence);

        final int worstTier = queryValues.size();

        final Tiers<Integer> tiers = new Tiers<>(worstTier);

        feedSequence.forEach(tiers::add);

        final ImmutableList.Builder<Integer> builder = ImmutableList.builder();

        int tier = worstTier;

        int previousTierOccupant = Integer.MIN_VALUE;

        do {
            final Integer tierOccupant = tiers.at(tier).get();

            MatcherAssert.assertThat(tierOccupant,
                                     greaterThanOrEqualTo(previousTierOccupant));

            builder.add(tierOccupant);

            previousTierOccupant = tierOccupant;
        } while (1 < tier--);

        final ImmutableList<Integer> arrangedByRank = builder.build();

        MatcherAssert.assertThat(arrangedByRank,
                                 containsInAnyOrder(queryValues.toArray()));
    }

    static {
        var maximallyShrunkTestCase =
                Optional.<Tuple2<ImmutableList<Integer>,
                        ImmutableList<Integer>>>empty();

        var builder =
                ImmutableList.<Tuple2<ImmutableList<Integer>,
                        ImmutableList<Integer>>>builder();

        try {
            HiddenTiersTest.testCases.withLimit(40).supplyTo(testCase -> {
                final ImmutableList<Integer> queryValues = testCase._1();
                final ImmutableList<Integer> feedSequence = testCase._2();

                try {
                    sharedTestLogic(queryValues, feedSequence);
                } finally {
                    builder.add(testCase);
                }
            });
        } catch (TrialsFactoring.TrialException e) {
            maximallyShrunkTestCase =
                    Optional.of((Tuple2<ImmutableList<Integer>,
                            ImmutableList<Integer>>) e.provokingCase());
        }

        canonicalMaximallyShrunkTestCase = maximallyShrunkTestCase;
        canonicalTestCases = builder.build();
    }

    @Test
    void parameterisedTestIntegrationViaTrialsTestAnnotation() {
        final var results =
                EngineTestKit.engine("junit-jupiter")
                             .selectors(DiscoverySelectors.selectClass(
                                     HiddenTiersTest.class))
                             .configurationParameter(
                                     "junit.jupiter.conditions.deactivate",
                                     "org.junit.*DisabledCondition")
                             .filters(TagFilter.includeTags(
                                     "parameterisedTest"))
                             .execute();
        final var events = results.testEvents();

        final var displayNames = events
                .stream()
                .filter(event -> EventType.FINISHED == event.getType())
                .map(Event::getTestDescriptor)
                .map(TestDescriptor::getDisplayName)
                .toArray(String[]::new);

        for (int index = 0; index < canonicalTestCases.size(); ++index) {
            MatcherAssert.assertThat(displayNames[index],
                                     StringContains.containsString(
                                             canonicalTestCases
                                                     .get(index)
                                                     .toString()));
        }

        final Optional<Tuple2<ImmutableList<Integer>, ImmutableList<Integer>>>
                cause = results
                .containerEvents()
                .filter(event -> EventType.FINISHED == event.getType())
                .filter(event -> event.getTestDescriptor() instanceof TestTemplateTestDescriptor)
                .findFirst()
                .flatMap(Event::getPayload)
                .flatMap(payload -> ((TestExecutionResult) payload)
                        .getThrowable())
                .map(throwable -> (Tuple2<ImmutableList<Integer>,
                        ImmutableList<Integer>>) (((TrialsFactoring.TrialException) throwable).provokingCase()));

        MatcherAssert.assertThat(cause,
                                 Matchers.equalTo(
                                         canonicalMaximallyShrunkTestCase));
    }

    @Test
    void dynamicTestFactoryIntegrationViaTestFactoryAnnotation() {
        final var results =
                EngineTestKit.engine("junit-jupiter")
                             .selectors(DiscoverySelectors.selectClass(
                                     HiddenTiersTest.class))
                             .configurationParameter(
                                     "junit.jupiter.conditions.deactivate",
                                     "org.junit.*DisabledCondition")
                             .filters(TagFilter.includeTags(
                                     "dynamicTestFactory"))
                             .execute();
        final var events = results.testEvents();

        final var displayNames = events
                .stream()
                .filter(event -> EventType.FINISHED == event.getType())
                .map(Event::getTestDescriptor)
                .map(TestDescriptor::getDisplayName)
                .toArray(String[]::new);

        for (int index = 0; index < canonicalTestCases.size(); ++index) {
            MatcherAssert.assertThat(displayNames[index],
                                     StringContains.containsString(
                                             canonicalTestCases
                                                     .get(index)
                                                     .toString()));
        }
    }

    // NOTE: IntelliJ complains that this nested class won't be discovered
    // and requires annotation with `@Nested`.
    // Don't do this; the idea is to *prevent* deliberately failing tests
    // from being run by default, these should
    // only be run by `parameterisedTestIntegrationViaTrialsTestAnnotation`.
    @Disabled
    static class HiddenTiersTest {
        private final static Trials<ImmutableList<Integer>> queryValueLists =
                api()
                        .integers(-1000, 1000)
                        .immutableLists()
                        .filter(list -> !list.isEmpty());

        private final static Trials<Tuple2<ImmutableList<Integer>,
                ImmutableList<Integer>>>
                testCases = queryValueLists
                .flatMap(queryValues -> {
                    final int minimumQueryValue =
                            queryValues.stream().min(Integer::compareTo).get();

                    // A background is a (possibly empty) run of values that are
                    // all less than the query values.
                    final Trials<ImmutableList<Integer>> backgrounds = api()
                            .integers(Integer.MIN_VALUE, minimumQueryValue - 1)
                            .immutableLists();

                    // A section is either a query value in a singleton list, or
                    // a background.
                    final List<Trials<ImmutableList<Integer>>> sectionTrials =
                            queryValues
                                    .stream()
                                    .flatMap(queryValue -> Stream.of(api().only(
                                                                             ImmutableList.of(
                                                                                     queryValue)),
                                                                     backgrounds))
                                    .collect(Collectors.toList());

                    sectionTrials.add(0, backgrounds);

                    // Glue the trials together and flatten the sections they
                    // yield into a single feed sequence per trial.
                    final Trials<ImmutableList<Integer>> feedSequences =
                            api().immutableLists(sectionTrials)
                                 .map(sections -> {
                                     final ImmutableList.Builder<Integer>
                                             builder = ImmutableList
                                             .builder();
                                     sections.forEach(builder::addAll);
                                     return builder.build();
                                 });
                    return feedSequences.map(feedSequence -> Tuple.tuple(
                            queryValues,
                            feedSequence));
                });

        @Tag("parameterisedTest")
        @TrialsTest(trials = "testCases", casesLimit = 40)
        void parameterisedTest(ImmutableList<Integer> queryValues,
                               ImmutableList<Integer> feedSequence) {
            sharedTestLogic(queryValues, feedSequence);
        }

        @Tag("dynamicTestFactory")
        @TestFactory
        Iterator<DynamicTest> dynamicTestFactory() {
            return JUnit5.dynamicTests(testCases.withLimit(40), (testCase -> {
                final ImmutableList<Integer> queryValues = testCase._1();
                final ImmutableList<Integer> feedSequence = testCase._2();

                sharedTestLogic(queryValues, feedSequence);
            }));
        }

    }
}
