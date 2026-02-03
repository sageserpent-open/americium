package com.sageserpent.americium.java;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

public class DelegatingTrials {
    public static <Case, SpecialisedTrials extends Trials<Case>> SpecialisedTrials delegateTo(
            Class<? extends SpecialisedTrials> specialisedTrialsClass,
            Trials<Case> underlying)
            throws InstantiationException, IllegalAccessException {


        final var byteBuddy = new ByteBuddy()
                .subclass(specialisedTrialsClass)
                .method(ElementMatchers
                                .isPublic()
                                .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(
                                        specialisedTrialsClass))))
                .intercept(MethodCall
                                   .invokeSelf()
                                   .on(underlying)
                                   .withAllArguments());

        return byteBuddy
                .make()
                .load(DelegatingTrials.class.getClassLoader())
                .getLoaded().newInstance();
    }
}