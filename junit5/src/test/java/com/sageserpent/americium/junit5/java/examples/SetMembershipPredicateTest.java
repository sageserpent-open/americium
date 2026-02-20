package com.sageserpent.americium.junit5.java.examples;

import com.google.common.collect.ImmutableList;
import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.junit5.java.TrialsTest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Disabled;

import java.util.function.Predicate;

/**
 * This is expected to fail, it is used as an example of feeding multiple
 * parameters to a test.
 */
@Disabled
public class SetMembershipPredicateTest {
    private final static Trials<ImmutableList<Long>> lists =
            Trials.api().longs().immutableLists();

    private final static Trials<Long> longs = Trials.api().longs();

    @TrialsTest(trials = {"lists", "longs", "lists"}, casesLimit = 10)
    void setMembershipShouldBeRecognisedByThePredicate(
            ImmutableList<Long> leftHandList, long additionalLongToSearchFor,
            ImmutableList<Long> rightHandList) {
        final Predicate<Long> systemUnderTest =
                new PoorQualitySetMembershipPredicate<>(ImmutableList
                                                                .<Long>builder()
                                                                .addAll(leftHandList)
                                                                .add(additionalLongToSearchFor)
                                                                .addAll(rightHandList)
                                                                .build());

        MatcherAssert.assertThat(systemUnderTest.test(additionalLongToSearchFor),
                                 CoreMatchers.is(true));
    }
}
