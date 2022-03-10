package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import cyclops.companion.Streams;
import cyclops.data.tuple.Tuple2;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class TrialsTestExtension
        implements TestTemplateInvocationContextProvider,
        InvocationInterceptor {
    private final Iterator<Long> cases;
    private final InlinedCaseFiltration inlinedCaseFiltration;

    private final static Class<? extends Throwable>[]
            additionalExceptionsToHandleAsFiltration =
            ImmutableSet
                    .of(TestAbortedException.class)
                    .stream()
                    .toArray(Class[]::new);

    {
        final Trials<Long> trials = Trials.api().longs();
        final int limit = 50;

        final Tuple2<Iterator<Long>, InlinedCaseFiltration> pair =
                trials.withLimit(limit).testIntegration();

        cases = pair._1();
        inlinedCaseFiltration = pair._2();
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext context) {
        return Streams
                .stream(cases)
                .map(caze -> new TestTemplateInvocationContext() {
                    @Override
                    public String getDisplayName(int invocationIndex) {
                        return String.format("%s %s",
                                             TestTemplateInvocationContext.super.getDisplayName(
                                                     invocationIndex),
                                             caze);
                    }

                    @Override
                    public List<Extension> getAdditionalExtensions() {
                        return ImmutableList.of(new ParameterResolver() {
                            @Override
                            public boolean supportsParameter(
                                    ParameterContext parameterContext,
                                    ExtensionContext extensionContext)
                                    throws ParameterResolutionException {
                                return parameterContext
                                        .getParameter()
                                        .getType()
                                        .isAssignableFrom(Long.class);
                            }

                            @Override
                            public Object resolveParameter(
                                    ParameterContext parameterContext,
                                    ExtensionContext extensionContext)
                                    throws ParameterResolutionException {
                                return caze;
                            }
                        });
                    }
                });
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) {
        if (!inlinedCaseFiltration.executeInFiltrationContext(() -> {
            try {
                InvocationInterceptor.super.interceptTestMethod(
                        invocation,
                        invocationContext,
                        extensionContext);
            } catch (Throwable e) {
                ExceptionUtils.throwAsUncheckedException(e);
            }
        }, additionalExceptionsToHandleAsFiltration))
            throw new TestAbortedException();
    }
}
