package com.sageserpent.americium.java;

import com.google.common.base.Preconditions;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static scala.jdk.javaapi.DurationConverters.toJava;

/**
 * Strategy used to limit the emission of cases by the implementation of
 * {@link Trials}. These are supplied by client code when calling
 * {@link Trials#withStrategy(Function)}.
 *
 * @apiNote Instances are expected to be stateful, so they should not be
 * reused when calling the aforementioned overloads.
 */
public interface CasesLimitStrategy extends com.sageserpent.americium.CasesLimitStrategy {
    /**
     * Limits test case emission using a time budget that starts when the
     * strategy is first consulted via {@link CasesLimitStrategy#moreToDo()}.
     *
     * @param timeBudget How long to allow a testing cycle to continue to
     *                   emit cases.
     * @return A fresh strategy instance - the time budget is not consumed
     * until the first call to {@link CasesLimitStrategy#moreToDo()}.
     */
    static CasesLimitStrategy timed(final Duration timeBudget) {
        return new CasesLimitStrategy() {
            Instant deadline = Instant.MAX;

            @Override
            public boolean moreToDo() {
                if (deadline.equals(Instant.MAX)) {
                    deadline = Instant.now().plus(timeBudget);
                }

                return !Instant.now().isAfter(deadline);
            }

            @Override
            public void noteRejectionOfCase() {

            }

            @Override
            public void noteEmissionOfCase() {

            }

            @Override
            public void noteStarvation() {

            }
        };
    }

    /**
     * Limits test case emission using a time budget that starts when the
     * strategy is first consulted via {@link CasesLimitStrategy#moreToDo()}.
     *
     * @param timeBudget How long to allow a testing cycle to continue to
     *                   emit cases.
     * @return A fresh strategy instance - the time budget is not consumed
     * until the first call to {@link CasesLimitStrategy#moreToDo()}.
     */
    static CasesLimitStrategy timed(
            final scala.concurrent.duration.FiniteDuration timeBudget) {
        return timed(toJava(timeBudget));
    }

    /**
     * Emulation of Scalacheck's approach to limiting emission of test cases.
     *
     * @param maximumNumberOfCases   *Upper* limit on the number of cases
     *                               emitted. <b>For Scalacheck aficionados:
     *                               the name reflects the fact that this is
     *                               a limit, contrast with Scalacheck's
     *                               {@code minSuccessfulTests}.</b>
     * @param maximumStarvationRatio Maximum ratio of case starvation versus
     *                               case emission.
     * @return A fresh strategy instance.
     * @implNote Like Scalacheck, the strategy will allow {@code
     * maximumNumberOfCases * maximumStarvationRatio} starvation to take
     * place before giving up.
     */
    static CasesLimitStrategy counted(int maximumNumberOfCases,
                                      double maximumStarvationRatio) {
        return new CasesLimitStrategy() {
            int numberOfCasesEmitted = 0;
            int starvationCount = 0;

            {
                Preconditions.checkArgument(0 <= maximumNumberOfCases);
                Preconditions.checkArgument(0 <= maximumStarvationRatio);
            }

            @Override
            public boolean moreToDo() {
                return maximumNumberOfCases > numberOfCasesEmitted &&
                       starvationCount <=
                       maximumNumberOfCases * maximumStarvationRatio;
            }

            @Override
            public void noteRejectionOfCase() {
                numberOfCasesEmitted -= 1;
                starvationCount += 1;
            }

            @Override
            public void noteEmissionOfCase() {
                numberOfCasesEmitted += 1;
            }

            @Override
            public void noteStarvation() {
                starvationCount += 1;
            }
        };
    }
}
