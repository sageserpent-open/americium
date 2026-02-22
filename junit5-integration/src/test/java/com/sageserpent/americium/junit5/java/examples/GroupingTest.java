package com.sageserpent.americium.junit5.java.examples;

import com.google.common.collect.ImmutableList;
import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsScaffolding;
import com.sageserpent.americium.junit5.java.ConfiguredTrialsTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;

import java.util.List;

/**
 * This is expected to fail, it is used as an example of finding a failure
 * and shrinking the failing test case.
 */
@Disabled
class GroupingTest {
    private static final TrialsScaffolding.SupplyToSyntax<ImmutableList<Integer>>
            testConfiguration = Trials
            .api()
            .integers(1, 10)
            .immutableLists()
            .withLimit(15);

    @ConfiguredTrialsTest("testConfiguration")
    void groupingShouldNotLoseOrGainElements(List<Integer> integerList) {
        final List<List<Integer>> groups =
                PoorQualityGrouping.groupsOfAdjacentDuplicates(integerList);

        final int size =
                groups.stream().map(List::size).reduce(Integer::sum).orElse(0);

        MatcherAssert.assertThat(size, Matchers.equalTo(integerList.size()));
    }
}