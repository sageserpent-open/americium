package com.sageserpent.americium.java.examples.junit5;

import com.google.common.collect.ImmutableList;
import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsScaffolding;
import com.sageserpent.americium.java.junit5.ConfiguredTrialsTest;
import org.junit.jupiter.api.Disabled;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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

        assertThat(size, equalTo(integerList.size()));
    }
}