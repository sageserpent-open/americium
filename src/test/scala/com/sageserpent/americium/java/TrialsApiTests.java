package com.sageserpent.americium.java;

import com.sageserpent.americium.Trials;
import org.junit.jupiter.api.Test;

public class TrialsApiTests {
    @Test
    void testDrivePureJavaApi() {
        final TrialsApi api = Trials.api();

        final Trials<Integer> integerTrials = api.only(1);
        final Trials<Double> doubleTrials = api.choose(new Double[]{1.2, 5.6});

        final Trials<? extends Number> alternateTrials = api.alternate(integerTrials, doubleTrials, doubleTrials);

        alternateTrials.supplyTo(number -> {
            System.out.println(number.floatValue());
        });
    }
}
