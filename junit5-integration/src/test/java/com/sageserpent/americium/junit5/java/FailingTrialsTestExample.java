package com.sageserpent.americium.junit5.java;

import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

@Disabled
public class FailingTrialsTestExample {
    private final static TrialsApi api = Trials.api();

    public static final Trials<Integer> integers = api.choose(1, 2, 3, 4, 5);

    public static int failingCase = 1;

    @Tag("failingTest")
    @TrialsTest(trials = "integers", casesLimit = 5)
    void failingTest(int caze) {
        if (caze == failingCase) {
            throw new RuntimeException("Deliberate failure for caze " + caze);
        }
    }
}
