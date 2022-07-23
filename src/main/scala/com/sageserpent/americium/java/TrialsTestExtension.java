package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import cyclops.companion.Streams;
import cyclops.data.tuple.Tuple2;
import cyclops.data.tuple.Tuple3;
import cyclops.data.tuple.Tuple4;
import lombok.Value;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.TestAbortedException;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrialsTestExtension
        implements TestTemplateInvocationContextProvider {
    private final static Class<? extends Throwable>[]
            additionalExceptionsToHandleAsFiltration =
            ImmutableSet
                    .of(TestAbortedException.class)
                    .stream()
                    .toArray(Class[]::new);
    private final static ImmutableList<TupleAdaptation<?>>
            tupleAdaptations =
            ImmutableList.of(new TupleAdaptation<>(Tuple2.class,
                                                   (tuple) -> Collections.unmodifiableList(
                                                           Arrays.asList(
                                                                   tuple.toArray()))),
                             new TupleAdaptation<>(Tuple3.class,
                                                   (tuple) -> Collections.unmodifiableList(
                                                           Arrays.asList(
                                                                   tuple.toArray()))),
                             new TupleAdaptation<>(Tuple4.class,
                                                   (tuple) -> Collections.unmodifiableList(
                                                           Arrays.asList(
                                                                   tuple.toArray()))));


    private static Iterator<TestIntegrationContext<Object>> testIntegrationContexts(
            ExtensionContext context) {
        final Method testMethod = context.getRequiredTestMethod();

        return AnnotationSupport
                .findAnnotation(testMethod, TrialsTest.class)
                .map(annotation -> {
                    final List<Trials<Object>> trials =
                            Lists.transform(instancesReferredToBy(Stream
                                                                          .of(annotation.trials())
                                                                          .collect(
                                                                                  Collectors.toList()),
                                                                  context,
                                                                  TrialsScaffolding.class),
                                            TrialsScaffolding::trials);

                    final Trials<List<Object>> lists = Trials
                            .api()
                            .collections(trials,
                                         () -> new Builder<Object,
                                                 List<Object>>() {
                                             // This builder tolerates null
                                             // elements, which is why we use
                                             // `.collections` here instead
                                             // of `.lists`, which would seem
                                             // to be the natural  choice.
                                             final List<Object> buffer =
                                                     new LinkedList<>();

                                             @Override
                                             public void add(Object caze) {
                                                 buffer.add(caze);
                                             }

                                             @Override
                                             public List<Object> build() {
                                                 return Collections.unmodifiableList(
                                                         buffer);
                                             }
                                         });

                    return (TrialsScaffolding.SupplyToSyntax) lists
                            .withLimits(annotation.casesLimit(),
                                        TrialsScaffolding.OptionalLimits
                                                .builder()
                                                .complexity(annotation.complexity())
                                                .shrinkageAttempts(
                                                        annotation.shrinkageAttempts())
                                                .build());
                })
                .orElseGet(() -> AnnotationSupport
                        .findAnnotation(testMethod,
                                        ConfiguredTrialsTest.class)
                        .map(annotation -> instancesReferredToBy(
                                Stream
                                        .of(annotation.value())
                                        .collect(
                                                Collectors.toList()),
                                context,
                                TrialsScaffolding.SupplyToSyntax.class).get(
                                0))
                        .orElseThrow(() -> {
                            throw new TestAbortedException(String.format(
                                    "`TrialsTest` annotation missing from" +
                                    " " +
                                    "method: %s",
                                    testMethod));
                        })).testIntegrationContexts();
    }

    private static <Clazz> List<Clazz> instancesReferredToBy(
            List<String> supplierFieldNames, ExtensionContext context,
            Class<? extends Clazz> clazz) {

        final Class<?> testClass = context.getRequiredTestClass();

        final List<Field> supplierFields =
                ReflectionSupport.findFields(testClass,
                                             field -> supplierFieldNames.contains(
                                                     field.getName()),
                                             HierarchyTraversalMode.BOTTOM_UP);

        final Map<String, Field> fieldsByName = supplierFields
                .stream()
                .filter(field -> clazz.isAssignableFrom(field.getType()))
                .collect(Collectors.toMap(Field::getName, Function.identity()));


        final Object testInstance = context.getTestInstance().orElse(null);

        return supplierFieldNames.stream().map(fieldName -> {
            try {
                final Field candidateField =
                        fieldsByName.get(fieldName);
                return (Clazz) Optional
                        .ofNullable(candidateField)
                        .flatMap(field -> ReflectionSupport
                                .tryToReadFieldValue(field,
                                                     testInstance)
                                .toOptional())
                        .orElseThrow(() ->
                                             supplierFields
                                                     .stream()
                                                     .filter(field -> fieldName.equals(
                                                             field.getName()))
                                                     .findFirst()
                                                     .map(field -> new RuntimeException(
                                                             String.format(
                                                                     "Field " +
                                                                     "of name" +
                                                                     " `%s` " +
                                                                     "in test" +
                                                                     " class " +
                                                                     "`%s` " +
                                                                     "has the" +
                                                                     " wrong " +
                                                                     "type of" +
                                                                     " `%s` -" +
                                                                     " should" +
                                                                     " be " +
                                                                     "typed " +
                                                                     "as a %s.",
                                                                     fieldName,
                                                                     testClass,
                                                                     field.getType(),
                                                                     clazz)))
                                                     .orElse(new RuntimeException(
                                                             String.format(
                                                                     "Failed " +
                                                                     "to find" +
                                                                     " field " +
                                                                     "of name: `%s` in test class `%s`.",
                                                                     fieldName,
                                                                     testClass)))
                        );
            } catch (Exception e) {
                ExceptionUtils.throwAsUncheckedException(e);
                return null;
            }
        }).collect(Collectors.toList());
    }

    private static List<Object> wrap(Object listOrSingleItem) {
        if (listOrSingleItem instanceof List)
            return (List<Object>) listOrSingleItem;
        else {
            final List<Object>
                    buffer =
                    new LinkedList<>();
            buffer.add(listOrSingleItem);

            return Collections.unmodifiableList(
                    buffer);
        }
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext context) {
        final Method method = context.getRequiredTestMethod();
        final Class<?>[] formalParameterTypes = method.getParameterTypes();

        return Streams
                .stream(testIntegrationContexts(context))
                .map(testIntegrationContext -> new TestTemplateInvocationContext() {
                    @Override
                    public String getDisplayName(int invocationIndex) {
                        final String shrinkagePrefix =
                                testIntegrationContext.isPartOfShrinkage()
                                ? "Shrinking ... " : "";

                        final List<Object> caze =
                                wrap(testIntegrationContext.caze());
                        return String.format("%s%s %s",
                                             shrinkagePrefix,
                                             TestTemplateInvocationContext.super.getDisplayName(
                                                     invocationIndex),
                                             1 <
                                             caze.size()
                                             ? caze
                                             :
                                             caze.get(0));
                    }

                    @Override
                    public List<Extension> getAdditionalExtensions() {
                        final List<Object> adaptedArguments =
                                new LinkedList<>();

                        final Map<Integer, TupleAdaptation<Object>>
                                cachedTupleAdaptations = new HashMap<>();

                        {
                            // Behold, Java in all its glory....
                            final AtomicInteger formalParameterIndex =
                                    new AtomicInteger(0);

                            final Iterator<Object> argumentIterator =
                                    wrap(testIntegrationContext.caze()).iterator();

                            while (formalParameterTypes.length >
                                   formalParameterIndex.get() &&
                                   argumentIterator.hasNext()) {
                                final Object argument =
                                        argumentIterator.next();
                                final Class<?> formalParameterType =
                                        formalParameterTypes[formalParameterIndex.get()];

                                final List<Object> expansion =
                                        cachedTupleAdaptations.computeIfAbsent(
                                                formalParameterIndex.get(),
                                                unused ->
                                                        formalParameterType.isInstance(
                                                                argument) ?
                                                        (TupleAdaptation<Object>) TupleAdaptation.fallbackForNonTuples
                                                                          :
                                                        (TupleAdaptation<Object>) tupleAdaptations
                                                                .stream()
                                                                .filter(tupleAdaptation -> tupleAdaptation.type.isInstance(
                                                                        argument))
                                                                .findFirst()
                                                                .orElse(TupleAdaptation.fallbackForNonTuples)).expansion.apply(
                                                argument);
                                formalParameterIndex.addAndGet(expansion.size());
                                adaptedArguments.addAll(expansion);
                            }
                        }

                        return ImmutableList.of(new ParameterResolver() {
                            @Override
                            public boolean supportsParameter(
                                    ParameterContext parameterContext,
                                    ExtensionContext extensionContext)
                                    throws ParameterResolutionException {
                                return Optional
                                        .ofNullable(adaptedArguments
                                                            .get(parameterContext.getIndex()))
                                        .map(parameter -> {
                                            final Class<?> formalParameterType =
                                                    parameterContext
                                                            .getParameter()
                                                            .getType();
                                            final Class<?>
                                                    formalParameterReferenceType =
                                                    formalParameterType.isPrimitive()
                                                    ? MethodType
                                                            .methodType(
                                                                    formalParameterType)
                                                            .wrap()
                                                            .returnType()
                                                    : formalParameterType;
                                            return formalParameterReferenceType.isInstance(
                                                    parameter);
                                        })
                                        .orElse(true);
                            }

                            @Override
                            public Object resolveParameter(
                                    ParameterContext parameterContext,
                                    ExtensionContext extensionContext)
                                    throws ParameterResolutionException {
                                return adaptedArguments.get(
                                        parameterContext.getIndex());
                            }
                        }, new InvocationInterceptor() {
                            @Override
                            public void interceptTestTemplateMethod(
                                    Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) {
                                if (!testIntegrationContext
                                        .inlinedCaseFiltration()
                                        .executeInFiltrationContext(
                                                () -> {
                                                    try {
                                                        InvocationInterceptor.super.interceptTestMethod(
                                                                invocation,
                                                                invocationContext,
                                                                extensionContext);
                                                    } catch (Throwable throwable) {
                                                        ExceptionUtils.throwAsUncheckedException(
                                                                throwable);
                                                    }
                                                },
                                                additionalExceptionsToHandleAsFiltration))
                                    throw new TestAbortedException();
                            }
                        }, new TestWatcher() {
                            @Override
                            public void testFailed(ExtensionContext context,
                                                   Throwable cause) {
                                testIntegrationContext
                                        .caseFailureReporting()
                                        .report(cause);
                            }
                        });
                    }
                });
    }

    @Value
    static class TupleAdaptation<Tuple> {
        public static TupleAdaptation<?> fallbackForNonTuples =
                new TupleAdaptation<>(Object.class, Collections::singletonList);

        Class<Tuple> type;
        Function<Tuple, List<Object>> expansion;
    }
}
