package com.sageserpent.americium.junit5.java;

import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

@Disabled
public class FailingTrialsTestExample {
    private final static TrialsApi api = Trials.api();

    public static final Trials<Integer> integers = api.integers(1, 10);

    @Tag("failingTest")
    @TrialsTest(trials = "integers", casesLimit = 5)
    void failingTest(int caze) {
        if (caze == 1) {
            throw new RuntimeException("Deliberate failure for caze 1");
        }
    }
}
