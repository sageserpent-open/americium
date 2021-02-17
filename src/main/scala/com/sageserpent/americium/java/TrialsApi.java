package com.sageserpent.americium.java;

public interface TrialsApi {
    <Case> Trials<Case> only(Case onlyCase);

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
    <Case> Trials<Case> choose(Case firstChoice,
                               Case secondChoice,
                               Case... otherChoices);

    <Case> Trials<Case> choose(Iterable<Case> choices);

    <Case> Trials<Case> choose(Case[] choices);

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
    <Case> Trials<Case> alternate(Trials<? extends Case> firstAlternative,
                                  Trials<? extends Case> secondAlternative,
                                  Trials<? extends Case>... otherAlternatives);

    <Case> Trials<Case> alternate(Iterable<Trials<Case>> alternatives);

    <Case> Trials<Case> alternate(Trials<Case>[] alternatives);
}
