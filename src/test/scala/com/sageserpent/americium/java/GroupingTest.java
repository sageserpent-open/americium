package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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