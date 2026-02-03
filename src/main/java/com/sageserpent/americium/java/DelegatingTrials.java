package com.sageserpent.americium.java;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

public class DelegatingTrials {
    private static final Cache<Class<? extends Trials<?>>, Class<?
            extends Trials<?>>>
            cache = Caffeine.newBuilder().build();

    public interface StateForDelegation<Case> {
        void delegateTo(Trials<Case> underlying);
    }

    public static <Case, SpecialisedTrials extends Trials<Case>> SpecialisedTrials delegateTo(
            Class<? extends SpecialisedTrials> specialisedTrialsClass,
            Trials<Case> underlying)
            throws InstantiationException, IllegalAccessException {


        final var clazz = cache.get(specialisedTrialsClass, superClazz -> {
            final var byteBuddy = new ByteBuddy()
                    .subclass(specialisedTrialsClass)
                    .name(String.format("DelegatingImplementationFor%1s",
                                        specialisedTrialsClass.getSimpleName()))
                    .defineField("delegatedTo",
                                 specialisedTrialsClass.getInterfaces()[0])
                    .implement(StateForDelegation.class)
                    .method(ElementMatchers
                                    .isPublic()
                                    .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(
                                            specialisedTrialsClass))))
                    .intercept(MethodCall
                                       .invokeSelf()
                                       .onField("delegatedTo")
                                       .withAllArguments())
                    .method(ElementMatchers.named("delegateTo")).intercept(
                            FieldAccessor.ofField("delegatedTo"));

            return byteBuddy
                    .make()
                    .load(DelegatingTrials.class.getClassLoader())
                    .getLoaded();
        });

        @SuppressWarnings("unchecked") final var stateForDelegation =
                (StateForDelegation<Case>) clazz.newInstance();

        stateForDelegation.delegateTo(underlying);

        @SuppressWarnings("unchecked") final var trials =
                (SpecialisedTrials) stateForDelegation;

        return trials;
    }
}