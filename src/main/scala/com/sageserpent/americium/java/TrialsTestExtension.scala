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
import scala.collection.mutable

object TrialsTestExtension {
  val simpleWrapping: TupleAdaptation[AnyRef] = {
    new TupleAdaptation[AnyRef] {
      override def clazz: Class[AnyRef] = classOf[AnyRef]
      override def expand(potentialTuple: AnyRef): Seq[AnyRef] =
        Seq(potentialTuple)
    }
  }
  protected val tupleExpansions: List[TupleAdaptation[_ <: AnyRef]] =
    List(
      new TupleAdaptation[JavaTuple2[_, _]] {
        override def clazz: Class[JavaTuple2[_, _]] = classOf[JavaTuple2[_, _]]
        override def expand(potentialTuple: JavaTuple2[_, _]): Seq[AnyRef] =
          potentialTuple.toArray
      },
      new TupleAdaptation[JavaTuple3[_, _, _]] {
        override def clazz: Class[JavaTuple3[_, _, _]] =
          classOf[JavaTuple3[_, _, _]]
        override def expand(
            potentialTuple: JavaTuple3[_, _, _]
        ): Seq[AnyRef] = potentialTuple.toArray
      },
      new TupleAdaptation[JavaTuple4[_, _, _, _]] {
        override def clazz: Class[JavaTuple4[_, _, _, _]] =
          classOf[JavaTuple4[_, _, _, _]]
        override def expand(
            potentialTuple: JavaTuple4[_, _, _, _]
        ): Seq[AnyRef] = potentialTuple.toArray
      }
    )
  private val additionalExceptionsToHandleAsFiltration
      : Array[Class[_ <: Throwable]] =
    Array(classOf[TestAbortedException])

  private def wrap(listOrSingleItem: AnyRef) = listOrSingleItem match {
    case _: util.List[_] => listOrSingleItem.asInstanceOf[util.List[AnyRef]]
    case _               => Collections.singletonList(listOrSingleItem)
  }

  trait TupleAdaptation[-PotentialTuple <: AnyRef] {
    def clazz: Class[_ >: PotentialTuple]
    def expand(potentialTuple: PotentialTuple): Seq[AnyRef]
  }
}

class TrialsTestExtension extends SoonToBeRemovedTrialsTestExtension {
  import TrialsTestExtension.*

  override def supportsTestTemplate(context: ExtensionContext) = true

  override def provideTestTemplateInvocationContexts(
      context: ExtensionContext
  ): Stream[TestTemplateInvocationContext] = {
    val method               = context.getRequiredTestMethod
    val formalParameterTypes = method.getParameterTypes

    def extractedArguments(
        testIntegrationContext: TestIntegrationContext[AnyRef]
    ): Array[Any] = {
      // Ported from Java code, and staying with that style...
      val adaptedArguments = new mutable.ArrayBuffer[AnyRef]

      {
        val cachedTupleAdaptations =
          new mutable.HashMap[Integer, TupleAdaptation[AnyRef]]
        var formalParameterIndex = 0
        val argumentIterator     = wrap(testIntegrationContext.caze).iterator

        while ({
          formalParameterTypes.length > formalParameterIndex && argumentIterator.hasNext
        }) {
          val argument = argumentIterator.next
          val formalParameterType =
            formalParameterTypes(formalParameterIndex)
          val expansion = cachedTupleAdaptations
            .getOrElseUpdate(
              formalParameterIndex, {
                // NOTE: don't use pattern matching on `argument` here - we want
                // to adapt based on the *formal* argument type, not on the
                // actual runtime type (which may implement additional
                // interfaces).
                if (formalParameterType.isInstance(argument))
                  simpleWrapping
                else
                  tupleExpansions
                    .find(_.clazz.isInstance(argument))
                    .getOrElse(simpleWrapping)
                    .asInstanceOf[TupleAdaptation[AnyRef]]
              }
            )
            .expand(argument)
          formalParameterIndex += expansion.size
          adaptedArguments.addAll(expansion)
        }
      }

      adaptedArguments.toArray
    }
    Streams
      .stream(testIntegrationContexts(context))
      .map((testIntegrationContext: TestIntegrationContext[AnyRef]) =>
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
            val adaptedArguments = extractedArguments(testIntegrationContext)

            ImmutableList.of(
              new ParameterResolver() {
                override def supportsParameter(
                    parameterContext: ParameterContext,
                    extensionContext: ExtensionContext
                ): Boolean = Option(
                  adaptedArguments(parameterContext.getIndex)
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
                ): Any = adaptedArguments(parameterContext.getIndex)
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
