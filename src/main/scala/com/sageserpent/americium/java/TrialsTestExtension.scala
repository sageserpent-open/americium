package com.sageserpent.americium.java
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.java.SoonToBeRemovedTrialsTestExtension.testIntegrationContexts
import cyclops.companion.Streams
import cyclops.data.tuple.{
  Tuple2 as JavaTuple2,
  Tuple3 as JavaTuple3,
  Tuple4 as JavaTuple4
}
import org.junit.jupiter.api.extension.*
import org.junit.platform.commons.util.ExceptionUtils
import org.opentest4j.TestAbortedException

import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.util
import java.util.Collections
import java.util.stream.Stream

class TrialsTestExtension extends SoonToBeRemovedTrialsTestExtension {
  private val additionalExceptionsToHandleAsFiltration
      : Array[Class[_ <: Throwable]] =
    Set(classOf[TestAbortedException]).toArray

  private def wrap(listOrSingleItem: Any) = if (
    listOrSingleItem.isInstanceOf[util.List[_]]
  ) listOrSingleItem.asInstanceOf[util.List[Any]]
  else Collections.singletonList(listOrSingleItem)

  case class TupleAdaptation[Tuple](
      clazz: Class[Tuple],
      expansion: Tuple => util.List[Any]
  )

  val fallbackForNonTuples: TupleAdaptation[Any] =
    TupleAdaptation(classOf[Any], Collections.singletonList[Any])

  protected val tupleAdaptations: ImmutableList[TupleAdaptation[_]] =
    ImmutableList.of(
      new TupleAdaptation[JavaTuple2[_, _]](
        classOf[JavaTuple2[_, _]],
        (tuple: JavaTuple2[_, _]) =>
          Collections.unmodifiableList(util.Arrays.asList(tuple.toArray: _*))
      ),
      new TupleAdaptation[JavaTuple3[_, _, _]](
        classOf[JavaTuple3[_, _, _]],
        (tuple: JavaTuple3[_, _, _]) =>
          Collections.unmodifiableList(util.Arrays.asList(tuple.toArray: _*))
      ),
      new TupleAdaptation[JavaTuple4[_, _, _, _]](
        classOf[JavaTuple4[_, _, _, _]],
        (tuple: JavaTuple4[_, _, _, _]) =>
          Collections.unmodifiableList(util.Arrays.asList(tuple.toArray: _*))
      )
    )

  override def supportsTestTemplate(context: ExtensionContext) = true

  override def provideTestTemplateInvocationContexts(
      context: ExtensionContext
  ): Stream[TestTemplateInvocationContext] = {
    val method               = context.getRequiredTestMethod
    val formalParameterTypes = method.getParameterTypes
    Streams
      .stream(testIntegrationContexts(context))
      .map((testIntegrationContext: TestIntegrationContext[Any]) =>
        new TestTemplateInvocationContext() {
          override def getDisplayName(invocationIndex: Int): String = {
            val shrinkagePrefix =
              if (testIntegrationContext.isPartOfShrinkage) "Shrinking ... "
              else ""
            val caze = wrap(testIntegrationContext.caze)
            String.format(
              "%s%s %s",
              shrinkagePrefix,
              super.getDisplayName(invocationIndex),
              if (1 < caze.size) caze
              else caze.get(0)
            )
          }

          override def getAdditionalExtensions: util.List[Extension] = {
            val adaptedArguments = new util.LinkedList[Any]
            val cachedTupleAdaptations =
              new util.HashMap[Integer, TupleAdaptation[
                Any
              ]]
            var formalParameterIndex = 0
            val argumentIterator = wrap(testIntegrationContext.caze).iterator
            while ({
              formalParameterTypes.length > formalParameterIndex && argumentIterator.hasNext
            }) {
              val argument = argumentIterator.next
              val formalParameterType =
                formalParameterTypes(formalParameterIndex)
              val expansion = cachedTupleAdaptations
                .computeIfAbsent(
                  formalParameterIndex,
                  (unused: Integer) =>
                    if (formalParameterType.isInstance(argument))
                      fallbackForNonTuples
                        .asInstanceOf[TupleAdaptation[
                          Any
                        ]]
                    else
                      tupleAdaptations.stream
                        .filter(
                          (tupleAdaptation: TupleAdaptation[
                            _
                          ]) => tupleAdaptation.clazz.isInstance(argument)
                        )
                        .findFirst
                        .orElse(fallbackForNonTuples)
                        .asInstanceOf[TupleAdaptation[
                          Any
                        ]]
                )
                .expansion
                .apply(argument)
              formalParameterIndex += expansion.size
              adaptedArguments.addAll(expansion)
            }

            ImmutableList.of(
              new ParameterResolver() {
                override def supportsParameter(
                    parameterContext: ParameterContext,
                    extensionContext: ExtensionContext
                ): Boolean = Option(
                  adaptedArguments.get(parameterContext.getIndex)
                ).forall((parameter: Any) => {
                  val formalParameterType =
                    parameterContext.getParameter.getType
                  val formalParameterReferenceType =
                    if (formalParameterType.isPrimitive)
                      MethodType
                        .methodType(formalParameterType)
                        .wrap
                        .returnType
                    else formalParameterType
                  formalParameterReferenceType.isInstance(parameter)
                })
                override def resolveParameter(
                    parameterContext: ParameterContext,
                    extensionContext: ExtensionContext
                ): Any = adaptedArguments.get(parameterContext.getIndex)
              },
              new InvocationInterceptor() {
                override def interceptTestTemplateMethod(
                    invocation: InvocationInterceptor.Invocation[Void],
                    invocationContext: ReflectiveInvocationContext[Method],
                    extensionContext: ExtensionContext
                ): Unit = {
                  if (
                    !testIntegrationContext.inlinedCaseFiltration
                      .executeInFiltrationContext(
                        () => {
                          try
                            super.interceptTestMethod(
                              invocation,
                              invocationContext,
                              extensionContext
                            )
                          catch {
                            case throwable: Throwable =>
                              ExceptionUtils
                                .throwAsUncheckedException(throwable)
                          }
                        },
                        additionalExceptionsToHandleAsFiltration
                      )
                  ) throw new TestAbortedException
                }
              },
              new TestWatcher() {
                override def testFailed(
                    context: ExtensionContext,
                    cause: Throwable
                ): Unit = {
                  testIntegrationContext.caseFailureReporting.report(cause)
                }
              }
            )
          }
        }
      )
  }
}
