package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;
import cyclops.companion.Streams;
import cyclops.data.tuple.Tuple2;
import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class SpikeTestExtension
        implements TestTemplateInvocationContextProvider,
        TestExecutionExceptionHandler,
        TestWatcher {
    private final Iterator<Long> cases;
    private final InlinedCaseFiltration inlinedCaseFiltration;

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
    public void handleTestExecutionException(ExtensionContext context,
                                             Throwable throwable)
            throws Throwable {
        try {
            throw throwable;
        } catch (SpecialException specialException) {
            throw new TestAbortedException();
        }
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        inlinedCaseFiltration.reject();

        TestWatcher.super.testAborted(context, cause);
    }

    public static class SpecialException extends RuntimeException {
    }
}
