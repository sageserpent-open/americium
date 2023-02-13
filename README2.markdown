# Americium - **_Property based testing for Java and Scala! Automatic test case shrinkage! Bring your own test style._**

[![Maven Central](https://index.scala-lang.org/sageserpent-open/americium/americium/latest-by-scala-version.svg?color=2465cd&style=flat)](https://index.scala-lang.org/sageserpent-open/americium/americium)

## Example

Some code we're not sure about...

```java
public class PoorQualityGrouping {
    // Where has this implementation gone wrong? Surely we've thought of
    // everything?
    public static <Element> List<List<Element>> groupsOfAdjacentDuplicates(
            List<Element> elements) {
        final Iterator<Element> iterator = elements.iterator();

        final List<List<Element>> result = new LinkedList<>();

        final LinkedList<Element> chunk = new LinkedList<>();

        while (iterator.hasNext()) {
            final Element element = iterator.next();

            // Got to clear the chunk when the element changes...
            if (!chunk.isEmpty() && chunk.get(0) != element) {
                // Got to add the chunk to the result before it gets cleared
                // - and watch out for empty chunks...
                if (!chunk.isEmpty()) result.add(chunk);
                chunk.clear();
            }

            // Always add the latest element to the chunk...
            chunk.add(element);
        }

        // Don't forget to add the last chunk to the result - as long as it's
        // not empty...
        if (!chunk.isEmpty()) result.add(chunk);

        return result;
    }
}
```

Let's test it - we'll use the integration with JUnit5 here...

```java
class GroupingTest {
    private static final TrialsScaffolding.SupplyToSyntax<ImmutableList<Integer>>
            testConfiguration = Trials
            .api()
            .integers(1, 10)
            .immutableLists()
            .withLimit(15);

    @ConfiguredTrialsTest("testConfiguration")
    void groupingShouldNotLoseOrGainElements(List<Integer> integerList) {
        final List<List<Integer>> groups =
                PoorQualityGrouping.groupsOfAdjacentDuplicates(integerList);

        final int size =
                groups.stream().map(List::size).reduce(Integer::sum).orElse(0);

        assertThat(size, equalTo(integerList.size()));
    }
}
```

What happens?

- Americium runs the same test repeatedly against different test case inputs, and finds a failing test case. Oh dear...

![](./screenshots/FailingExample.png)

- The first failing test case leads to an automatic shrinkage process that yields a maximally shrunk test case. See how
  the failing test case's values lie between 1 and 10, just as specified in the test. Shrinking respects the constraints
  we configured into our test data...

![](./screenshots/Shrinkage.png)

- Americium also tells us what the maximally shrunk test case was and how to reproduce it immediately when we re-run the
  test...

```
Case:
[1, 1, 2]
Reproduce via Java property:
trials.recipeHash=3b2a3709bf92b8551b2e9ae0b8b6d526
Reproduce via Java property:
trials.recipe="[{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":2}},{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":1}},{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":1}},{\"ChoiceOf\":{\"index\":0}}]"
```

![](./screenshots/Reproduction.png)

Now go and fix it! (_HINT:_ `final LinkedList<Element> chunk = new LinkedList<>();` Why final? What was the intent? Do
the Java collections work that way? Maybe the test expectations should have been more stringent?)

## Goals

- Agnostic - as long as your test takes a test case, throws an exception when it fails and completes normally otherwise,
  it can be used with Americium.
- Lightweight - there is no provided assertion language or property DSL; Americium is about building test cases,
  supplying them to a test and shrinking down failing test cases. Your tests, your style of writing them.
- Suitable for Java and Scala - there are two APIs, each optimised for the choice of language.
- Shrinkage is automatic and respects test case invariants. You don't write shrinkage code and your shrunk test cases
  conform to how you want them built.

In addition, there are some enhancements to the Scala `Random` class that might also pique your interest, but go see for
yourself in the code, it's simple enough...

## Cookbook ##

- Start with a trials api specialized for Java or Scala.
- Coax some trials instances out of the api - either use the factory methods that give you canned trials instances, or
  specify your own cases to choose from (either with equal probability or with weights), or hard-wire in some single
  value.
- Transform them by mapping.
- Combine them together by flat-mapping.
- Filter out what you don't want.
- You can alternate between different ways of making the same shape of case data, either with equal probability or with
  weights.
- Use helper methods to make a trials from some collection out of a simpler trials for the collection's elements.
- Once you've built up the right kind of trials instance, put it to use: specify an upper limit for the number of cases
  you want to examine and feed them to your test code. When your test code throws an exception, the trials machinery
  will try to shrink down whatever test case caused it.

### Java ###

```java


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.math.BigInteger;
import java.time.*;

class Cookbook {
    /* Start with a trials api for Java. */

    private final static TrialsApi api = Trials.api();

    /*
     Coax some trials instances out of the api...
     ... either use the factory methods that give you canned trials instances
      ...
    */

    final Trials<Integer> integers = api.integers();

    final Trials<String> strings = api.strings();

    final Trials<Instant> instants = api.instants();

    /*
     ... or specify your own cases to choose from ...
     ... either with equal probability ...
    */

    final Trials<Color> colors = api.choose(Color.RED, Color.GREEN, Color.BLUE);

    /* ... or with weights ... */

    final Trials<String> elementsInTheHumanBody = api.chooseWithWeights(
            Maps.immutableEntry(65,
                                "Oxygen"),
            Maps.immutableEntry(18,
                                "Carbon"),
            Maps.immutableEntry(10,
                                "Hydrogen"),
            Maps.immutableEntry(3,
                                "Nitrogen"));

    /* ... or hard-wire in some single value. */

    final Trials<Object> thisIsABitEmbarrassing = api.only(null);

    /* Transform them by mapping. */

    final Trials<Integer> evenNumbers = integers.map(integral -> 2 * integral);

    final Trials<ZoneId> zoneIds =
            api
                    .choose("UTC",
                            "Europe/London",
                            "Asia/Singapore",
                            "Atlantic/Madeira")
                    .map(ZoneId::of);

    /* Combine them together by flat-mapping. */

    final Trials<ZonedDateTime> zonedDateTimes =
            instants.flatMap(instant -> zoneIds.map(zoneId -> ZonedDateTime.ofInstant(
                    instant,
                    zoneId)));

    /* Filter out what you don't want. */

    final Trials<ZonedDateTime> notOnASunday = zonedDateTimes.filter(
            zonedDateTime -> !zonedDateTime
                    .toOffsetDateTime()
                    .getDayOfWeek()
                    .equals(DayOfWeek.SUNDAY));

    /*
     You can alternate between different ways of making the same shape 
     case data...
     ... either with equal probability ...
    */

    final Trials<Rectangle2D> rectangles =
            api.doubles().flatMap(x -> api.doubles().flatMap(
                    y -> api
                            .doubles()
                            .flatMap(w -> api
                                    .doubles()
                                    .map(h -> new Rectangle2D.Double(x,
                                                                     y,
                                                                     w,
                                                                     h)))));

    final Trials<Ellipse2D> ellipses =
            api.doubles().flatMap(x -> api.doubles().flatMap(
                    y -> api
                            .doubles()
                            .flatMap(w -> api
                                    .doubles()
                                    .map(h -> new Ellipse2D.Double(x,
                                                                   y,
                                                                   w,
                                                                   h)))));

    final Trials<Shape> shapes = api.alternate(rectangles, ellipses);

    /* ... or with weights. */

    final Trials<BigInteger> likelyToBePrime = api.alternateWithWeights(
            Maps.immutableEntry(10,
                                api
                                        .choose(1, 3, 5, 7, 11, 13, 17, 19)
                                        .map(BigInteger::valueOf)),
            // Mostly from this pool of small primes - nice and quick.
            Maps.immutableEntry(1,
                                api
                                        .longs()
                                        .map(BigInteger::valueOf)
                                        .map(BigInteger::nextProbablePrime))
            // Occasionally we want a big prime and will pay the cost of 
            // computing it.
    );

    /* Use helper methods to make a trials from some collection out of a simpler
     trials for the collection's elements. */

    final Trials<ImmutableList<Shape>> listsOfShapes = shapes.immutableLists();

    final Trials<ImmutableSortedSet<BigInteger>> sortedSetsOfPrimes =
            likelyToBePrime.immutableSortedSets(BigInteger::compareTo);

    /*
     Once you've built up the right kind of trials instance, put it to
     use: specify an upper limit for the number of cases you want to examine
     and feed them to your test code. When your test code throws an exception,
     the trials machinery will try to shrink down whatever test case caused it.
    */

    @Test
    public void theExtraDayInALeapYearIsJustNotToleratedIfItsNotOnASunday() {
        notOnASunday.withLimit(50).supplyTo(when -> {
            final LocalDate localDate = when.toLocalDate();

            try {
                assert !localDate.getMonth().equals(Month.FEBRUARY) ||
                       localDate.getDayOfMonth() != 29;
            } catch (AssertionError exception) {
                System.out.println(when);   // Watch the shrinkage in action!
                throw exception;
            }
        });
    }
}
```

### Scala ###

```scala
import com.sageserpent.americium.Trials.api

import org.scalatest.flatspec.AnyFlatSpec

import java.awt.geom.{Ellipse2D, Rectangle2D}
import java.awt.{List => _, _}
import java.math.BigInteger
import java.time._
import scala.collection.immutable.SortedSet

class Cookbook extends AnyFlatSpec {
  /* Coax some trials instances out of the api...
   * ... either use the factory methods that give you canned trials instances
   * ... */
  val integers: Trials[Int] = api.integers

  val strings: Trials[String] = api.strings

  val instants: Trials[Instant] = api.instants

  /* ... or specify your own cases to choose from ...
   * ... either with equal probability ... */

  val colors: Trials[Color] =
    api.choose(Color.RED, Color.GREEN, Color.BLUE)

  /* ... or with weights ... */

  val elementsInTheHumanBody: Trials[String] = api.chooseWithWeights(
    65 -> "Oxygen",
    18 -> "Carbon",
    10 -> "Hydrogen",
    3 -> "Nitrogen"
  )

  /* ... or hard-wire in some single value. */

  val thisIsABitEmbarrassing: Trials[Null] = api.only(null)

  /* Transform them by mapping. */

  val evenNumbers: Trials[Int] = integers.map(integral => 2 * integral)

  val zoneIds: Trials[ZoneId] = api
    .choose("UTC", "Europe/London", "Asia/Singapore", "Atlantic/Madeira")
    .map(ZoneId.of)

  /* Combine them together by flat-mapping. */

  val zonedDateTimes: Trials[ZonedDateTime] =
    for {
      instant <- instants
      zoneId <- zoneIds
    } yield ZonedDateTime.ofInstant(instant, zoneId)

  /* Filter out what you don't want. */

  val notOnASunday: Trials[ZonedDateTime] =
    zonedDateTimes.filter(_.toOffsetDateTime.getDayOfWeek != DayOfWeek.SUNDAY)

  /* You can alternate between different ways of making the same shape case
   * data...
   * ... either with equal probability ... */

  val rectangles: Trials[Rectangle2D.Double] =
    for {
      x <- api.doubles
      y <- api.doubles
      w <- api.doubles
      h <- api.doubles
    } yield new Rectangle2D.Double(x, y, w, h)

  val ellipses: Trials[Ellipse2D.Double] = for {
    x <- api.doubles
    y <- api.doubles
    w <- api.doubles
    h <- api.doubles
  } yield new Ellipse2D.Double(x, y, w, h)

  val shapes: Trials[Shape] = api.alternate(rectangles, ellipses)

  /* ... or with weights. */

  val likelyToBePrime: Trials[BigInt] = api.alternateWithWeights(
    10 -> api
      .choose(1, 3, 5, 7, 11, 13, 17, 19)
      .map(
        BigInt.apply
      ), // Mostly from this pool of small primes - nice and quick.
    1 -> api.longs
      .map(BigInteger.valueOf)
      .map(
        _.nextProbablePrime: BigInt
      ) // Occasionally we want a big prime and will pay the cost of computing it.
  )

  /* Use helper methods to make a trials from some collection out of a simpler
   * trials for the collection's elements. */

  val listsOfShapes: Trials[List[Shape]] =
    shapes.lists

  val sortedSetsOfPrimes: Trials[SortedSet[_ <: BigInt]] =
    likelyToBePrime.sortedSets

  /* Once you've built up the right kind of trials instance, put it to use:
   * specify an upper limit for the number of cases you want to examine and feed
   * them to your test code. When your test code throws an exception, the trials
   * machinery will try to shrink down whatever test case caused it. */

  "the extra day in a leap year" should "not be tolerated if its not on a Sunday" in {
    notOnASunday
      .withLimit(50)
      .supplyTo { when =>
        val localDate = when.toLocalDate
        try
          assert(
            !(localDate.getMonth == Month.FEBRUARY) || localDate.getDayOfMonth != 29
          )
        catch {
          case exception =>
            println(when) // Watch the shrinkage in action!

            throw exception
        }
      }
  }
}

```



