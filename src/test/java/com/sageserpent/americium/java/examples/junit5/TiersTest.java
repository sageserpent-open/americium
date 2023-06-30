package com.sageserpent.americium.java.examples.junit5;

import com.google.common.collect.ImmutableList;
import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.junit5.TrialsTest;
import cyclops.data.tuple.Tuple;
import cyclops.data.tuple.Tuple2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sageserpent.americium.java.Trials.api;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class TiersTest {
    private final static Trials<ImmutableList<Integer>> queryValueLists = api()
            .integers(1, 10)
            .flatMap(numberOfChoices -> api()
                    .integers(-1000, 1000)
                    .immutableListsOfSize(
                            numberOfChoices)
                    .flatMap(choices -> api()
                            .choose(choices)
                            .immutableListsOfSize(
                                    numberOfChoices)));


    private final static Trials<Tuple2<ImmutableList<Integer>,
            ImmutableList<Integer>>>
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
                        queryValues
                                .stream()
                                .flatMap(queryValue ->
                                                 Stream.of(api().only(
                                                                   ImmutableList.of(
                                                                           queryValue)),
                                                           backgrounds))
                                .collect(Collectors.toList());

                sectionTrials.add(0, backgrounds);

                // Glue the trials together and flatten the sections they
                // yield into a single feed sequence per trial.
                final Trials<ImmutableList<Integer>> feedSequences =
                        api().immutableLists(sectionTrials).map(sections -> {
                            final ImmutableList.Builder<Integer> builder =
                                    ImmutableList.builder();
                            sections.forEach(builder::addAll);
                            return builder.build();
                        });
                return feedSequences.map(feedSequence -> Tuple.tuple(queryValues,
                                                                     feedSequence));
            });

    @TrialsTest(trials = "testCases", casesLimit = 30)
    void tiersShouldRetainTheLargestElements(ImmutableList<Integer> queryValues,
                                             ImmutableList<Integer> feedSequence) {
        System.out.format("Query values: %s, feed sequence: %s\n",
                          queryValues,
                          feedSequence);

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
    }
}
