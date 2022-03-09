package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;
import cyclops.companion.Streams;
import cyclops.data.tuple.Tuple2;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class SpikeTestExtension
        implements TestTemplateInvocationContextProvider,
        TestExecutionExceptionHandler,
        TestWatcher,
        InvocationInterceptor {
    private final Iterator<Long> cases;
    private final InlinedCaseFiltration inlinedCaseFiltration;
    private final RuntimeException rejectionByInlineFilter =
            new RuntimeException();

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
                                            ExtensionContext extensionContext)
            throws Throwable {
        com.sageserpent.americium.Trials
                .throwInlineFilterRejection()
                .withValue(() -> {
                               throw rejectionByInlineFilter;
                           },
                           () -> {
                               try {
                                   InvocationInterceptor.super.interceptTestMethod(
                                           invocation,
                                           invocationContext,
                                           extensionContext);
                               } catch (Throwable e) {
                                   ExceptionUtils.throwAsUncheckedException(e);
                               }

                               return null;
                           });
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context,
                                             Throwable throwable)
            throws Throwable {
        throw rejectionByInlineFilter == throwable ? new TestAbortedException()
                                                   : throwable;
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        inlinedCaseFiltration.reject();

        TestWatcher.super.testAborted(context, cause);
    }
}
