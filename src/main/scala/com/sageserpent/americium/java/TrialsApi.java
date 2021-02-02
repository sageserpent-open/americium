package com.sageserpent.americium.java;

public interface TrialsApi {
    TrialsApi api();

    <SomeCase> Trials<SomeCase> only(SomeCase onlyCase);

    /**
     * Produce a trials instance that chooses between several cases.
     * <p>
     * NOTE: the peculiar signature is to avoid ambiguity with the overloads for an iterable / array of cases.
     *
     * @param firstChoice  Mandatory first choice, so there is at least one case.
     * @param secondChoice Mandatory second choice, so there is always some element of choice.
     * @param otherChoices Optional further choices.
     * @return The trials instance.
     */
    <SomeCase> Trials<SomeCase> choose(SomeCase firstChoice,
                                       SomeCase secondChoice,
                                       SomeCase... otherChoices);

    <SomeCase> Trials<SomeCase> choose(Iterable<SomeCase> choices);

    <SomeCase> Trials<SomeCase> choose(SomeCase[] choices);

    /**
     * Produce a trials instance that alternates between the cases of the given alternatives.
     * <p>
     * NOTE: the peculiar signature is to avoid ambiguity with the overloads for an iterable / array of cases.
     *
     * @param firstAlternative  Mandatory first alternative, so there is at least one trials.
     * @param secondAlternative Mandatory second alternative, so there is always some element of choice.
     * @param otherAlternatives Optional further alternatives.
     * @return The trials instance.
     */
    <SomeCase> Trials<SomeCase> alternate(Trials<? extends SomeCase> firstAlternative,
                                          Trials<? extends SomeCase> secondAlternative,
                                          Trials<? extends SomeCase>... otherAlternatives);

    <SomeCase> Trials<SomeCase> alternate(Iterable<Trials<SomeCase>> alternatives);

    <SomeCase> Trials<SomeCase> alternate(Trials<SomeCase>[] alternatives);
}
