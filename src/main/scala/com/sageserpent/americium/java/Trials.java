package com.sageserpent.americium.java;

import com.google.common.collect.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public abstract class Trials<Case> implements TrialsFactoring<Case> {
    public static TrialsApi api() {
        return (TrialsApi) TrialsImplementation.javaApi();
    }

    public static <Result> Result whenever(Boolean satisfiedPrecondition, Supplier<Result> block) {
        return com.sageserpent.americium.Trials.whenever(satisfiedPrecondition, block::get);
    }

    public static void whenever(Boolean satisfiedPrecondition, Runnable block) {
        com.sageserpent.americium.Trials.whenever(satisfiedPrecondition, () -> {
            block.run();
            return null;
        });
    }

    abstract com.sageserpent.americium.Trials<Case> scalaTrials();

    public abstract <TransformedCase> Trials<TransformedCase> map(final Function<Case, TransformedCase> transform);

    public abstract <TransformedCase> Trials<TransformedCase> flatMap(final Function<Case, Trials<TransformedCase>> step);

    public abstract Trials<Case> filter(final Predicate<Case> predicate);

    public abstract <TransformedCase> Trials<TransformedCase> mapFilter(final Function<Case, Optional<TransformedCase>> filteringTransform);

    public abstract Trials.SupplyToSyntax<Case> withLimit(final int limit);

    public abstract Trials.SupplyToSyntax<Case> withRecipe(final String recipe);

    public abstract Trials<ImmutableList<Case>> immutableLists();

    public abstract Trials<ImmutableSet<Case>> immutableSets();

    public abstract Trials<ImmutableSortedSet<Case>> immutableSortedSets(final Comparator<Case> elementComparator);

    public abstract <Value> Trials<ImmutableMap<Case, Value>> immutableMaps(final Trials<Value> values);

    public abstract <Value> Trials<ImmutableSortedMap<Case, Value>> immutableSortedMaps(final Comparator<Case> elementComparator, final Trials<Value> values);

    public abstract Trials<ImmutableList<Case>> immutableListsOfSize(final int size);

    public interface SupplyToSyntax<Case> {
        void supplyTo(final Consumer<Case> consumer);

        Iterator<Case> asIterator();
    }
}


        