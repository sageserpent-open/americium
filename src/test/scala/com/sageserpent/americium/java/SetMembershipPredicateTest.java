package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;

import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SetMembershipPredicateTest {
    private final static Trials<ImmutableList<Long>> lists =
            Trials.api().longs().immutableLists();

    private final static Trials<Long> longs = Trials.api().longs();

    @TrialsTest(trials = {"lists", "longs", "lists"}, casesLimit = 10)
    void setMembershipShouldBeRecognisedByThePredicate(
            ImmutableList<Long> leftHandList, long additionalLongToSearchFor,
            ImmutableList<Long> rightHandList) {
        final Predicate<Long> systemUnderTest =
                new PoorQualitySetMembershipPredicate(ImmutableList
                                                              .builder()
                                                              .addAll(leftHandList)
                                                              .add(additionalLongToSearchFor)
                                                              .addAll(rightHandList)
                                                              .build());

        assertThat(systemUnderTest.test(additionalLongToSearchFor),
                   is(true));
    }
}
