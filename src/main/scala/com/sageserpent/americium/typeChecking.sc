
import com.sageserpent.americium.Trials

import java.util.function.{Consumer, Predicate, Function => JavaFunction}

val trials: Trials[Int] = new Trials[Int] {

  /**
      * Reproduce a specific case in a repeatable fashion, based on a recipe.
      *
      * @param recipe This encodes a specific case and will only be understood by the
      *               same *value* of trials instance that was used to obtain it.
      * @return The specific case denoted by the recipe.
      * @throws RuntimeException if the recipe does not correspond to the receiver,
      *                          either due to it being created by a different
      *                          flavour of trials instance or subsequent code changes.
      */
  override def reproduce(recipe: String) = ???

  override def supplyTo(consumer: Int => Unit): Unit = ???

  override def filter(predicate: Predicate[_ >: Int]) = ???

  /**
      * Consume trial cases until either there are no more or an exception is thrown by {@code consumer}.
      * If an exception is thrown, attempts will be made to shrink the trial case that caused the
      * exception to a simpler case that throws an exception - the specific kind of exception isn't
      * necessarily the same between the first exceptional case and the final simplified one. The exception
      * from the simplified case (or the original exceptional case if it could not be simplified) is wrapped
      * in an instance of {@link TrialException} which also contains the case that provoked the exception.
      *
      * @param consumer An operation that consumes a 'Case', and may throw an exception.
      */
  override def supplyTo(consumer: Consumer[_ >: Int]): Unit = ???

  /**
      * Consume the single trial case reproduced by a recipe. This is intended
      * to repeatedly run a test against a known failing case when debugging, so
      * the expectation is for this to *eventually* not throw an exception after
      * code changes are made in the system under test.
      *
      * @param recipe   This encodes a specific case and will only be understood by the
      *                 same *value* of trials instance that was used to obtain it.
      * @param consumer An operation that consumes a 'Case', and may throw an exception.
      * @throws RuntimeException if the recipe is not one corresponding to the receiver,
      *                          either due to it being created by a different flavour of trials instance.
      */
  override def supplyTo(recipe: String, consumer: Consumer[_ >: Int]): Unit =
    ???
}

val flatMappedTrials: Trials[Int] = trials flatMap ((integer =>
  Trials.only(integer)): JavaFunction[Int, Trials[Int]])

val mappedTrials
  : Trials[Double] = trials map ((_ * 2.5): JavaFunction[Int, Double])