package com.sageserpent.americium.java.examples;


import com.sageserpent.americium.java.TrialsScaffolding;
import org.junit.jupiter.api.Test;

import static com.sageserpent.americium.java.Trials.api;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class DoubleShrinkageTest {
    @Test
    void tryItOut() {
        try {
            api()
                    .doubles()
                    .withLimit(15)
                    .supplyTo(input -> {
                        try {
                            final double root = Math.sqrt(input);
                            assertThat(Double.isNaN(root), is(false));
                        } catch (Throwable throwable) {
                            System.out.println(input);
                            throw throwable;
                        }
                    });
        } catch (TrialsScaffolding.TrialException exception) {
            System.out.println(exception);
            assertThat((double) exception.provokingCase(), closeTo(0, 0.1));
        }
    }
}




