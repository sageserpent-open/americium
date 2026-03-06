package com.sageserpent.americium.junit5.java.examples;

import com.google.common.collect.ImmutableList;
import com.sageserpent.americium.java.Builder;
import com.sageserpent.americium.java.CasesLimitStrategy;
import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsScaffolding;
import cyclops.data.tuple.Tuple;
import cyclops.data.tuple.Tuple2;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sageserpent.americium.java.Trials.api;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Scratch {
    static class Tiers<Element extends Comparable<Element>> {
        final int worstTier;

        final List<Element> storage;

        public Tiers(int worstTier) {
            this.worstTier = worstTier;
            storage = new ArrayList<>(worstTier) {
                @Override
                public void add(int index, Element element) {
                    if (size() < worstTier) {
                        super.add(index, element);
                    } else if (0 < index) /* <<----- FIX */ {
                        for (int shiftDestination = 0;
                             shiftDestination < index - 1; /* <<----- FIX */
                             ++shiftDestination) {
                            super.set(shiftDestination,
                                      super.get(1 + shiftDestination));
                        }

                        super.set(index - 1, element); /* <<----- FIX */
                    }
                }
            };
        }

        void add(Element element) {
            final int index = Collections.binarySearch(storage, element);

            if (0 >= index) /* <<----- INJECTED FAULT */ {
                storage.add(-(index + 1), element);
            } else {
                storage.add(index, element);
            }
        }

        Optional<Element> at(int tier) {
            return 0 < tier && tier <= storage.size()
                   ? Optional.of(storage.get(storage.size() - tier))
                   :
                   Optional.empty();
        }
    }

    static class PoorQualitySetMembershipPredicate<Element extends Comparable<Element>> implements
            Predicate<Element> {
        private final Comparable[] elements;

        public PoorQualitySetMembershipPredicate(Collection<Element> elements) {
            this.elements = elements.toArray(Comparable[]::new);
        }

        @Override
        public boolean test(Element element) {
            return 0 <= Arrays.binarySearch(elements, element);
        }
    }

    public static void main(String[] args) {
        final Instant startTime = Instant.now();

        try {
            final String suffix = "are";

            final int suffixLength = suffix.length();

            api()
                    .characters('a', 'z')
                    .collections(Builder::stringBuilder)
                    .filter(caze -> caze.length() >
                                    suffixLength)
                    .withStrategy(cycle -> {
                        final Instant rightNow = Instant.now();

                        System.out.format("Elapsed time in seconds: %s, number of previous cycles:  %s\n",
                                          Duration
                                                  .between(startTime, rightNow)
                                                  .getSeconds(),
                                          cycle.numberOfPreviousCycles());

                        return CasesLimitStrategy.timed(
                                Duration.ofSeconds(5));
                    })
                    .supplyTo(input -> {
                        try {
                            assertThat(input, not(containsString(suffix)));
                        } catch (Throwable throwable) {
                            System.out.println(input);
                            throw throwable;
                        }
                    });
        } catch (TrialsScaffolding.TrialException exception) {
            final Instant rightNow = Instant.now();

            System.out.format("Total elapsed time in seconds: %s\n",
                              Duration
                                      .between(startTime, rightNow)
                                      .getSeconds());
            System.out.println(exception);
        }

    }

    public static void main2(String... args) {


        final Trials<ImmutableList<Integer>> queryValueLists = api()
                .integers(-1000, 1000)
                .immutableLists()
                .filter(list -> !list.isEmpty());

        final Trials<Tuple2<ImmutableList<Integer>, ImmutableList<Integer>>>
                testCases =
                queryValueLists.flatMap(queryValues -> {
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
                            queryValues.stream()
                                       .flatMap(queryValue ->
                                                        Stream.of(
                                                                api().only(
                                                                        ImmutableList.of(
                                                                                queryValue)),
                                                                backgrounds))
                                       .collect(Collectors.toList());

                    sectionTrials.add(0, backgrounds);

                    // Glue the trials together and flatten the sections they
                    // yield into a single feed sequence per trial.
                    final Trials<ImmutableList<Integer>> feedSequences =
                            api()
                                    .immutableLists(sectionTrials)
                                    .map(sections -> {
                                        final ImmutableList.Builder<Integer>
                                                builder =
                                                ImmutableList.builder();
                                        sections.forEach(builder::addAll);
                                        return builder.build();
                                    });

                    return feedSequences.map(feedSequence ->
                                                     Tuple.tuple(queryValues,
                                                                 feedSequence));
                });

        AtomicInteger count = new AtomicInteger();

        try {
            testCases.withLimit(11000).supplyTo(testCase -> {
                count.incrementAndGet();

                final ImmutableList<Integer> queryValues = testCase._1();
                final ImmutableList<Integer> feedSequence = testCase._2();

                final int worstTier = queryValues.size();
                final Tiers<Integer> tiers = new Tiers<>(worstTier);

                feedSequence.forEach(tiers::add);

                final ImmutableList.Builder<Integer> builder =
                        ImmutableList.builder();

                int tier = worstTier;

                int previousTierOccupant = Integer.MIN_VALUE;

                do {
                    final Integer tierOccupant = tiers.at(tier).get();

                    assertThat(tierOccupant,
                               greaterThanOrEqualTo(previousTierOccupant));

                    builder.add(tierOccupant);

                    previousTierOccupant = tierOccupant;
                } while (1 < tier--);

                final ImmutableList<Integer> arrangedByRank = builder.build();

                assertThat(arrangedByRank,
                           containsInAnyOrder(queryValues.toArray()));
            });
        } finally {
            System.out.println(count.get());
        }
    }
}
