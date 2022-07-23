package com.sageserpent.americium.java;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SoonToBeRemovedTrialsTestExtension
        implements TestTemplateInvocationContextProvider {


    protected static Iterator<TestIntegrationContext<Object>> testIntegrationContexts(
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
}
