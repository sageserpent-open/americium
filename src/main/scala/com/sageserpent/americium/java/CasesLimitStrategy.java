package com.sageserpent.americium.java;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
