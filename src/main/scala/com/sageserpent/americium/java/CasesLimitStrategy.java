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
 * {@link Trials#withStrategy(Function, Trials.OptionalLimits)} and
 * {@link Trials#withStrategy(Function, Trials.OptionalLimits, Trials.ShrinkageStop)}.
 *
 * @apiNote Instances are expected to be stateful, so they should not be
 * reused when calling the aforementioned overloads.
 */
public interface CasesLimitStrategy {
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

    /**
     * Query used by the implementation of {@link Trials} to control the
     * emission of new cases.
     *
     * @return True to signal that more cases should be emitted, false to
     * stop emission.
     * @apiNote Once a call returns false, there should be no further
     * interaction with the strategy by the implementation of {@link Trials}
     * except for additional calls to this method.
     */
    boolean moreToDo();

    /**
     * Notes that inlined case filtration in a test body has rejected a case.
     *
     * @apiNote This is *not* called when the filtration provided by
     * {@link Trials#filter(Predicate)} rejects a case.
     */
    void noteRejectionOfCase();

    /**
     * Notes that a case has been successfully emitted. The case is
     * guaranteed to be a new one that has *not* been emitted previously in a
     * call to {@link Trials.SupplyToSyntax#supplyTo(Consumer)}.
     */
    void noteEmissionOfCase();

    /**
     * Notes that a case has not been successfully emitted. This can be due
     * to it being a duplicate of an earlier case emitted previously in a
     * call to {@link Trials.SupplyToSyntax#supplyTo(Consumer)}, or may be
     * due to the filtration provided by {@link Trials#filter(Predicate)}
     * rejecting a case, or may be due to the complexity limit being breached.
     */
    void noteStarvation();
}
