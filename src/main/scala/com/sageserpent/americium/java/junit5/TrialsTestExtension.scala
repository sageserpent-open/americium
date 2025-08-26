package com.sageserpent.americium.java.junit5

import com.sageserpent.americium.Trials as ScalaTrials
import com.sageserpent.americium.java.{
  CaseFailureReporting,
  InlinedCaseFiltration,
  TestIntegrationContext,
  TrialsScaffolding
}
import com.sageserpent.americium.storage.RocksDBConnection
import cyclops.companion.Streams
import cyclops.data.tuple.{
  Tuple2 as JavaTuple2,
  Tuple3 as JavaTuple3,
  Tuple4 as JavaTuple4
}
import org.junit.jupiter.api.extension.*
import org.junit.platform.commons.support.{
  AnnotationSupport,
  HierarchyTraversalMode,
  ReflectionSupport
}
import org.junit.platform.engine.UniqueId
import org.opentest4j.TestAbortedException

import java.lang.invoke.MethodType
import java.lang.reflect.{Field, Method}
import java.util
.stream.Stream
import java.util.{Iterator as JavaIterator, List as JavaList}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

object TrialsTestExtension {
  val simpleWrapping: TupleAdaptation[AnyRef] = {
    new TupleAdaptation[AnyRef] {
      override def clazz: Class[AnyRef]                        = classOf[AnyRef]
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

  private def supplyToSyntax(
      context: ExtensionContext
  ): TrialsScaffolding.SupplyToSyntax[_ >: Vector[AnyRef]] = {
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
  }

  private def instancesReferredToBy[Clazz](
      supplierFieldNames: List[String],
      context: ExtensionContext,
      clazz: Class[Clazz]
  ): List[Clazz] = {
    val testClass      = context.getRequiredTestClass
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

  trait TrialTemplateInvocationContext extends TestTemplateInvocationContext {
    override def getAdditionalExtensions: JavaList[Extension] = List(
      parameterResolver,
      invocationInterceptor,
      testWatcher
    ).asJava

    private def parameterResolver: ParameterResolver =
      new ParameterResolver() {
        override def supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Boolean = {
          val parameterGuardedAgainstNullValue =
            Option(parameters(parameterContext.getIndex))

          parameterGuardedAgainstNullValue.forall((parameter: Any) => {
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
        }
        override def resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
        ): Any = parameters(parameterContext.getIndex)
      }

    protected def invocationInterceptor: InvocationInterceptor =
      new InvocationInterceptor() {
        override def interceptTestTemplateMethod(
            invocation: InvocationInterceptor.Invocation[Void],
            invocationContext: ReflectiveInvocationContext[Method],
            extensionContext: ExtensionContext
        ): Unit = {
          val eligible = inlinedCaseFiltration
            .executeInFiltrationContext(
              () =>
                super.interceptTestTemplateMethod(
                  invocation,
                  invocationContext,
                  extensionContext
                ),
              additionalExceptionsToHandleAsFiltration
            )

          if (!eligible) throw new TestAbortedException
        }
      }

    protected def inlinedCaseFiltration: InlinedCaseFiltration

    protected def caseFailureReporting: CaseFailureReporting

    protected def parameters: Array[AnyRef]

    protected def testWatcher: TestWatcher
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

    def extractedParameters(
        wrappedCase: Vector[AnyRef]
    ): Array[AnyRef] = {
      // Ported from Java code, and staying with that style...
      val adaptedParameters = new mutable.ArrayBuffer[AnyRef]

      {
        val cachedTupleAdaptations =
          new mutable.HashMap[Integer, TupleAdaptation[AnyRef]]
        var formalParameterIndex = 0
        val argumentIterator     = wrappedCase.iterator

        while (
          formalParameterTypes.length > formalParameterIndex && argumentIterator.hasNext
        ) {
          val parameter           = argumentIterator.next
          val formalParameterType =
            formalParameterTypes(formalParameterIndex)
          val expansion = cachedTupleAdaptations
            .getOrElseUpdate(
              formalParameterIndex, {
                // NOTE: don't use pattern matching on the parameter here - we
                // want to adapt based on the *formal* argument type, not on the
                // actual runtime type (which may implement additional
                // interfaces).
                if (formalParameterType.isInstance(parameter))
                  simpleWrapping
                else
                  tupleExpansions
                    .find(_.clazz.isInstance(parameter))
                    .getOrElse(simpleWrapping)
                    .asInstanceOf[TupleAdaptation[AnyRef]]
              }
            )
            .expand(parameter)
          formalParameterIndex += expansion.size
          adaptedParameters.addAll(expansion)
        }
      }

      adaptedParameters.toArray
    }

    val rocksDBConnection = RocksDBConnection.evaluation.value

    val replayedUniqueIds =
      LauncherDiscoveryListenerCapturingReplayedUniqueIds
        .replayedUniqueIds()
        .asScala

    val supply = supplyToSyntax(context)

    val casesAvailableForReplayByUniqueId: mutable.Map[UniqueId, AnyRef] =
      mutable.Map.from(
        replayedUniqueIds
          .flatMap(uniqueId =>
            rocksDBConnection
              .recipeFromUniqueId(uniqueId.toString)
              .map(uniqueId -> supply.reproduce(_).asInstanceOf[AnyRef])
          )
      )

    val haveReproducedTestCaseForAllReplayedUniqueIds =
      replayedUniqueIds.nonEmpty && casesAvailableForReplayByUniqueId.keys == replayedUniqueIds

    if (haveReproducedTestCaseForAllReplayedUniqueIds) {
      Streams.stream(new JavaIterator[TestTemplateInvocationContext] {
        override def hasNext: Boolean =
          casesAvailableForReplayByUniqueId.nonEmpty

        override def next(): TestTemplateInvocationContext =
          new TrialTemplateInvocationContext {
            override protected def inlinedCaseFiltration
                : InlinedCaseFiltration =
              (
                  runnable: Runnable,
                  additionalExceptionsToNoteAsFiltration: Array[
                    Class[_ <: Throwable]
                  ]
              ) => {
                val inlineFilterRejection = new RuntimeException

                try {
                  ScalaTrials.throwInlineFilterRejection.withValue(() =>
                    throw inlineFilterRejection
                  ) { runnable.run() }

                  true
                } catch {
                  case exception: RuntimeException
                      if inlineFilterRejection == exception =>
                    false
                  case throwable: Throwable
                      if additionalExceptionsToNoteAsFiltration.exists(
                        _.isInstance(throwable)
                      ) =>
                    throw throwable
                }
              }

            override protected def caseFailureReporting
                : CaseFailureReporting = {
              // NOTE: don't wrap the exception as we are doing replay; this
              // matches how all the failing trials bar the last have their
              // exceptions reported.
              throwable => throw throwable
            }

            override protected def parameters: Array[AnyRef] = {
              val potentialReplayedTestCase =
                TestExecutionListenerCapturingUniqueIds
                  .uniqueId()
                  .toScala
                  .flatMap(casesAvailableForReplayByUniqueId.get)

              potentialReplayedTestCase.fold(ifEmpty = Array.empty[AnyRef]) {
                testCase =>
                  extractedParameters(wrap(testCase))
              }
            }

            override def getDisplayName(
                invocationIndex: Int
            ): String = {
              val details =
                if (1 == casesAvailableForReplayByUniqueId.size)
                  casesAvailableForReplayByUniqueId
                    .get(casesAvailableForReplayByUniqueId.keys.head)
                    .getOrElse("")
                else ""

              s"${super.getDisplayName(invocationIndex)} $details"
            }

            override protected def invocationInterceptor
                : InvocationInterceptor = {
              val delegatedSuper = super.invocationInterceptor

              new InvocationInterceptor {
                override def interceptTestTemplateMethod(
                    invocation: InvocationInterceptor.Invocation[Void],
                    invocationContext: ReflectiveInvocationContext[Method],
                    extensionContext: ExtensionContext
                ): Unit = {
                  TestExecutionListenerCapturingUniqueIds
                    .uniqueId()
                    .ifPresent(casesAvailableForReplayByUniqueId.remove)

                  delegatedSuper.interceptTestTemplateMethod(
                    invocation,
                    invocationContext,
                    extensionContext
                  )
                }
              }
            }
            override protected def testWatcher: TestWatcher =
              new TestWatcher() {}
          }
      })
    } else
      Streams
        .stream(
          supply
            .testIntegrationContexts()
            .asInstanceOf[JavaIterator[TestIntegrationContext[AnyRef]]]
        )
        .map { testIntegrationContext =>
          new TrialTemplateInvocationContext {
            private val wrappedTestCase: Vector[AnyRef] =
              wrap(testIntegrationContext.caze)

            override protected def inlinedCaseFiltration
                : InlinedCaseFiltration =
              testIntegrationContext.inlinedCaseFiltration
            override protected def caseFailureReporting: CaseFailureReporting =
              testIntegrationContext.caseFailureReporting
            override protected val parameters: Array[AnyRef] =
              extractedParameters(wrappedTestCase)

            override def getDisplayName(invocationIndex: Int): String = {
              val shrinkagePrefix =
                if (testIntegrationContext.isPartOfShrinkage) "Shrinking ... "
                else ""

              val details =
                if (1 < wrappedTestCase.size) wrappedTestCase
                else wrappedTestCase(0)

              s"$shrinkagePrefix${super.getDisplayName(invocationIndex)} $details"
            }

            override protected def invocationInterceptor
                : InvocationInterceptor = {
              val delegatedSuper = super.invocationInterceptor

              new InvocationInterceptor {
                override def interceptTestTemplateMethod(
                    invocation: InvocationInterceptor.Invocation[Void],
                    invocationContext: ReflectiveInvocationContext[Method],
                    extensionContext: ExtensionContext
                ): Unit = {
                  // NOTE: it would be more consistent to use
                  // `TestExecutionListenerCapturingUniqueIds.uniqueId`, but we
                  // finally have the full unique id from `extensionContext`
                  // courtesy of JUnit5, so letuse it as intended.
                  rocksDBConnection.recordUniqueId(
                    extensionContext.getUniqueId,
                    testIntegrationContext.recipe
                  )

                  delegatedSuper.interceptTestTemplateMethod(
                    invocation,
                    invocationContext,
                    extensionContext
                  )
                }
              }
            }

            override protected def testWatcher: TestWatcher =
              new TestWatcher() {
                override def testFailed(
                    context: ExtensionContext,
                    cause: Throwable
                ): Unit = {
                  caseFailureReporting.report(cause)
                }
              }
          }
        }
  }
}
