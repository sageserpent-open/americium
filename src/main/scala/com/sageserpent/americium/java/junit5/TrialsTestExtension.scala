package com.sageserpent.americium.java.junit5

import com.sageserpent.americium.Trials as ScalaTrials
import com.sageserpent.americium.java.{TestIntegrationContext, TrialsScaffolding}
import cyclops.companion.Streams
import cyclops.data.tuple.{Tuple2 as JavaTuple2, Tuple3 as JavaTuple3, Tuple4 as JavaTuple4}
import org.junit.jupiter.api.extension.*
import org.junit.platform.commons.support.{AnnotationSupport, HierarchyTraversalMode, ReflectionSupport}
import org.opentest4j.TestAbortedException

import java.lang.invoke.MethodType
import java.lang.reflect.{Field, Method}
import java.util
import java.util.stream.Stream
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

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
      new TupleAdaptation[JavaTuple2[?, ?]] {
        override def clazz: Class[JavaTuple2[?, ?]] = classOf[JavaTuple2[?, ?]]
        override def expand(potentialTuple: JavaTuple2[?, ?]): Seq[AnyRef] =
          potentialTuple.toArray
      },
      new TupleAdaptation[JavaTuple3[?, ?, ?]] {
        override def clazz: Class[JavaTuple3[?, ?, ?]] =
          classOf[JavaTuple3[?, ?, ?]]
        override def expand(
            potentialTuple: JavaTuple3[?, ?, ?]
        ): Seq[AnyRef] = potentialTuple.toArray
      },
      new TupleAdaptation[JavaTuple4[?, ?, ?, ?]] {
        override def clazz: Class[JavaTuple4[?, ?, ?, ?]] =
          classOf[JavaTuple4[?, ?, ?, ?]]
        override def expand(
            potentialTuple: JavaTuple4[?, ?, ?, ?]
        ): Seq[AnyRef] = potentialTuple.toArray
      }
    )

  private val additionalExceptionsToHandleAsFiltration
      : Array[Class[_ <: Throwable]] =
    Array(classOf[TestAbortedException])

  protected def testIntegrationContexts(
      context: ExtensionContext
  ): util.Iterator[TestIntegrationContext[AnyRef]] = {
    val testMethod = context.getRequiredTestMethod

    AnnotationSupport
      .findAnnotation(testMethod, classOf[TrialsTest])
      .toScala
      .map((annotation: TrialsTest) => {
        val trials: Vector[ScalaTrials[AnyRef]] =
          instancesReferredToBy(
            annotation.trials.toList,
            context,
            classOf[TrialsScaffolding[
              AnyRef,
              _ <: TrialsScaffolding.SupplyToSyntax[AnyRef]
            ]]
          ).map(_.trials.scalaTrials()).toVector

        val vectors: ScalaTrials[Vector[AnyRef]] =
          ScalaTrials.api.sequences(trials)

        vectors.javaTrials
          .withLimit(annotation.casesLimit)
          .withComplexityLimit(annotation.complexity)
          .withShrinkageAttemptsLimit(annotation.shrinkageAttempts)
      })
      .getOrElse {
        AnnotationSupport
          .findAnnotation(testMethod, classOf[ConfiguredTrialsTest])
          .toScala
          .map((annotation: ConfiguredTrialsTest) =>
            instancesReferredToBy(
              List(annotation.value),
              context,
              classOf[TrialsScaffolding.SupplyToSyntax[AnyRef]]
            ).head
          )
          .getOrElse {
            throw new TestAbortedException(
              String.format(
                "`TrialsTest` annotation missing from method: %s",
                testMethod
              )
            )
          }
      }
      .testIntegrationContexts
      .asInstanceOf[util.Iterator[TestIntegrationContext[AnyRef]]]
  }

  private def instancesReferredToBy[Clazz](
      supplierFieldNames: List[String],
      context: ExtensionContext,
      clazz: Class[Clazz]
  ): List[Clazz] = {
    val testClass = context.getRequiredTestClass
    val supplierFields = ReflectionSupport
      .findFields(
        testClass,
        (field: Field) => supplierFieldNames.contains(field.getName),
        HierarchyTraversalMode.BOTTOM_UP
      )
      .asScala
    val fieldsByName = supplierFields
      .filter((field: Field) => clazz.isAssignableFrom(field.getType))
      .map(field => field.getName -> field)
      .toMap
    supplierFieldNames.map((fieldName: String) =>
      {
        val candidateField = fieldsByName.get(fieldName)
        candidateField
          .flatMap((field: Field) =>
            ReflectionSupport
              .tryToReadFieldValue(field, context.getTestInstance.orElse(null))
              .toOptional
              .toScala
          )
          .getOrElse {
            throw supplierFields
              .find(fieldName == _.getName)
              .fold(ifEmpty = {
                new RuntimeException(
                  String.format(
                    "Failed to find field of name: `%s` in test class `%s`.",
                    fieldName,
                    testClass
                  )
                )
              })(field =>
                new RuntimeException(
                  String.format(
                    "Field of name `%s` in test class `%s` has the wrong type of `%s` - should be typed as a %s.",
                    fieldName,
                    testClass,
                    field.getType,
                    clazz
                  )
                )
              )
          }
      }.asInstanceOf[Clazz]
    )
  }

  private def wrap(listOrSingleItem: AnyRef): Vector[AnyRef] =
    listOrSingleItem match {
      case vector: Vector[AnyRef] => vector
      case _                      => Vector(listOrSingleItem)
    }

  trait TupleAdaptation[-PotentialTuple <: AnyRef] {
    def clazz: Class[_ >: PotentialTuple]
    def expand(potentialTuple: PotentialTuple): Seq[AnyRef]
  }
}

class TrialsTestExtension extends TestTemplateInvocationContextProvider {
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
              else caze(0)
            )
          }

          override def getAdditionalExtensions: util.List[Extension] = {
            val adaptedArguments = extractedArguments(testIntegrationContext)

            List(
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
                        () =>
                          super.interceptTestMethod(
                            invocation,
                            invocationContext,
                            extensionContext
                          ),
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
            ).asJava
          }
        }
      )
  }
}
