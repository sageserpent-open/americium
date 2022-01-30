# Americium - **_Test cases galore! Automatic case shrinkage! Bring your own test style. For Scala and Java..._**

[![Build Status](https://travis-ci.com/sageserpent-open/americium.svg?branch=master)](https://travis-ci.com/sageserpent-open/americium)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sageserpent/americium_2.13/badge.svg?style=flat&gav=true)](https://maven-badges.herokuapp.com/maven-central/com.sageserpent/americium_2.13/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sageserpent/americium_3/badge.svg?style=flat&gav=true)](https://maven-badges.herokuapp.com/maven-central/com.sageserpent/americium_3/badge.svg)

## What? Why? ##

You like writing parameterised tests - so you have a block of test code expressed as a test method or function of lambda
form, and something that pumps test cases into that code block as one or more arguments.

At this point, the likes of QuickCheck, FsCheck, Scalacheck and VavrTest come to mind, amongst others. If you are
working in Scala, then you'll probably be thinking of Scalacheck, maybe ZioTest, perhaps Hedgehog...? If in Java, then
Jqwik, JUnit-QuickCheck, or possibly VavrTest?

All great things - the author has had the benefit of using Scalacheck for several years on various Scala works, finding
all kinds of obscure, knotty bugs that would otherwise lay hidden until the fateful day in production. Likewise VavrTest
has helped for the Java works. Fun has been had with ZioTest and Hedgehog too...

However, one nagging problem with both Scalacheck and VavrTest is in the matter of test case shrinkage - it's all very
well when a parameterised test fails for some case, but reproducing and debugging the failure can be a real pain.

For one thing, not all frameworks allow direct reproduction of the offending test case - so if each individual test
execution for a piece of data takes appreciable time, then running the entire parameterised test up to the point of
failure can take minutes for more sophisticated tests. What's more, the test case that provokes the test failure may be
extraordinarily complex; these frameworks all use the notion of building up test cases based on combining randomly
varying data into bigger and bigger chunks, which often means that whatever provokes a failure is buried in a complex
test case with additional random data that is of no relevance to the failure.

For example, if we are testing a stable sorting algorithm in Scala, we may find due to the use of weak equality in the
sort algorithm that it is not stable, so equivalent entries in the list are rearranged in order with respect to each
other:

```scala
import scala.math.Ordering

// Insertion sort, but with a bug...
def notSoStableSort[Element](
                              elements: List[Element]
                            )(implicit ordering: Ordering[Element]): List[Element] =
  elements match {
    case Nil => Nil
    case head :: tail =>
      // Spot the deliberate mistake......vvvv
      notSoStableSort(tail).span(ordering.lteq(_, head)) match {
        case (first, second) => first ++ (head :: second)
      }
  }

notSoStableSort(Nil: List[(Int, Int)])(
  Ordering.by(_._1)
) // List() - Hey - worked first time...
notSoStableSort(List(1 -> 2))(
  Ordering.by(_._1)
) // List((1,2)) - Yeah, check those edge cases!
notSoStableSort(List(1 -> 2, -1 -> 9))(
  Ordering.by(_._1)
) // List((-1,9), (1,2)) - Fancy a beer, anyone?
notSoStableSort(List(1 -> 2, -1 -> 9, 1 -> 3))(
  Ordering.by(_._1)
) // List((-1,9), (1,3), (1,2)) ? Uh? I wanted List((-1,9), (1,2), (1,3))!!!!
notSoStableSort(List(1 -> 2, 1 -> 3))(
  Ordering.by(_._1)
) // List((1,3), (1,2)) ? Huh! I wanted List((1,2), (1,3)) - going to be working overtime...

```

Now this isn't so painful because it's a toy problem and we know exactly where to start debugging, and therefore how to
minimise the test case (the last one is a minimal case, all we need to do is submit two entries that are not equal
by `==` but are in terms of the supplied ordering).

Think this is always going to be the case? Take a look at this one (currently unsolved, still using
Scalacheck): https://github.com/sageserpent-open/plutonium/issues/57 - in particular, look at the test failure logs on
the ticket. All that gibberish in the logs is *one single test case*. Want to try debugging through that? How would you
minimise it?

This is made all the worse by the rarity of this bug - in fact, Scalacheck used to use random seed values back when this
bug was first encountered, so the the test only failed once in a blue moon. To make this failure reproducible each time
means that the test has to run a *long, long* time. Even more fun if you're in a debugging session watching your
breakpoints being hit for several hundred successful cases before you get to the one that finally fails, whichever it
is...

What we want here is something that automatically shrinks a failing test case down to a minimal test case (or at least
reasonably close to one), and provides some way of reproducing this minimal test case without having to slog through a
whole bunch of successful cases we aren't interested in.

After toiling through quite a few of these monster test failures in the Plutonium, Curium and several commercial
projects, the author decided to address this issue.

To be fair, there are some frameworks out there that also offer automatic test case shrinkage - your mileage may vary.
Scalacheck does this, but with caveats: https://github.com/typelevel/scalacheck/pull/440. ZioTest does this too, give it
a whirl and see how you fare. So does Hedgehog for that matter...

This brings us to the next pain point for the author, which is the extent to which the framework has opinions about how
your code is to be structured. Scalacheck comes not only with generation of test cases, but its own property-checking
DSL and style of assembling a test suite, which you may or may not buy into. There is an integration into Scalatest so
that you can supply test cases to a Scalatest test - perhaps you might like that better? MUnit will let you use
Scalacheck, but you are back to its own DSL ... or perhaps you'd prefer UTest - not sure what you'd do there...

... or maybe you write in Java and use JUnit? What then? VavrTest doesn't at time of writing offer any shrinkage
support.

What the author wanted was a framework that:

1. Offers automatic shrinkage to a minimal or nearly-minimal test case. __Yes, invariants are preserved on test case
   data.__
1. Shrinks efficiently.
1. Offers direct reproduction of a failing, minimised test case.
1. Covers finite combinations of atomic cases without duplication when building composite cases.
1. Gets out of the way of testing style - doesn't care about whether the tests are pure functional or imperative,
   doesn't offer a DSL or try to structure your test suite.
1. Supports Scala and Java as first class citizens.
1. Supports covariance of test case generation in Scala, so cases for a subclass can be substituted for cases for a
   supertrait/superclass.
1. Supports covariance of test case generation in Java, so cases for a subclass can be substituted for cases for a
   superinterface/superclass.
1. Allows automatic derivation of test case generation for sum/product types (aka case class hierarchies) in the spirit
   of Scalacheck Shapeless.

So, finally we come to `Trials` - which is what this library has to offer.

Well, that and some syntax enhancements to the Scala `Random` class that might also pique your interest, but go see for
yourself in the code, it's simple enough...

## Example ##

Let's take our sorting implementation above, write some proper parameterised tests and drive them via a `Trials`
instance ...

```scala

import com.sageserpent.americium.Trials.api // Start with the Scala api for `Trials`...

// We're going to sort a list of associations (key-value pairs) by the key...
val ordering = Ordering.by[(Int, Int), Int](_._1)

// Build up a trials instance for key value pairs by flat-mapping from simpler
// trials instances for the keys and values...
val keyValuePairs: Trials[(Int, Int)] = for {
  key <- api.choose(
    0 to 100
  ) // We want to encourage duplicated keys - so a key is always some integer from 0 up to but not including 100.
  value <-
    api.integers // A value on the other hand is any integer from right across the permissible range.
} yield key -> value

// Here's the trials instance we use to drive the tests for sorting...
val associationLists: Trials[List[(Int, Int)]] =
  keyValuePairs.lists // This makes a trials of lists out of the simpler trials of key-value pairs.

"stableSorting" should "sort according to the ordering" in
  associationLists
    .filter(
      _.nonEmpty
    ) // Filter out the empty case as we can't assert sensibly on it.
    .withLimit(200) // Only check up to 200 cases inclusive.
    .supplyTo { nonEmptyAssocationList: List[(Int, Int)] =>
      // This is a parameterised test, using `nonEmptyAssociationList` as the
      // test case parameter...
      val sortedResult = notSoStableSort(nonEmptyAssocationList)(ordering)

      // Using Scalatest assertions here...
      assert(
        sortedResult.zip(sortedResult.tail).forall((ordering.lteq _).tupled)
      )
    }

it should "conserve the original elements" in
  associationLists.withLimit(200).supplyTo {
    associationList: List[(Int, Int)] =>
      val sortedResult = notSoStableSort(associationList)(ordering)

      sortedResult should contain theSameElementsAs associationList
  }

// Until the bug is fixed, we expect this test to fail...
it should "also preserve the original order of the subsequences of elements that are equivalent according to the order" in
  associationLists.withLimit(200).supplyTo {
    associationList: List[(Int, Int)] =>
      Trials.whenever(
        associationList.nonEmpty
      ) // Filter out the empty case as while we can assert on it, the assertion would be trivial.
      {
        val sortedResult = notSoStableSort(associationList)(ordering)

        assert(sortedResult.groupBy(_._1) == associationList.groupBy(_._1))
      }
  }
```

Run the tests - the last one will fail with a nicely minimised case:

```org.scalatest.exceptions.TestFailedException: HashMap(46 -> List((46,0), (46,2))) did not equal HashMap(46 -> List((46,2), (46,0)))
Expected :HashMap(46 -> List((46,2), (46,0)))
Actual   :org.scalatest.exceptions.TestFailedException: HashMap(46 -> List((46,0), (46,2)))

Trial exception with underlying cause:
org.scalatest.exceptions.TestFailedException: HashMap(46 -> List((46,0), (46,2))) did not equal HashMap(46 -> List((46,2), (46,0)))
Case:
List((46,2), (46,0))
Reproduce with recipe:
[
.... block of JSON ....
]
```

We also see a JSON recipe for reproduction too further down in the output. We can use this recipe to make a temporary
bug-reproduction test that focuses solely on the test case causing the problem:

   ```scala
// Until the bug is fixed, we expect this test to fail...
it should "also preserve the original order of the subsequences of elements that are equivalent according to the order - this time with the failure reproduced directly" ignore
  associationLists
    .withRecipe(
      """[
        |    {
        |        "ChoiceOf" : {
        |            "index" : 1
        |        }
        |    },
        |    {
        |        "ChoiceOf" : {
        |            "index" : 46
        |        }
        |    },
        |    {
        |        "FactoryInputOf" : {
        |            "input" : 0
        |        }
        |    },
        |    {
        |        "ChoiceOf" : {
        |            "index" : 1
        |        }
        |    },
        |    {
        |        "ChoiceOf" : {
        |            "index" : 46
        |        }
        |    },
        |    {
        |        "FactoryInputOf" : {
        |            "input" : 2
        |        }
        |    },
        |    {
        |        "ChoiceOf" : {
        |            "index" : 0
        |        }
        |    }
        |]""".stripMargin)
    .supplyTo { associationList: List[(Int, Int)] =>
      val sortedResult = notSoStableSort(associationList)(ordering)

      assert(sortedResult.groupBy(_._1) == associationList.groupBy(_._1))
    } 
   ```

## Cookbook ##

- Start with a trials api for either Java or Scala.
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
        try assert(
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

## Rhetorical Questions ##

### How did this come about? ###

As mentioned in the introduction above, working on this issue in the Plutonium
project (https://github.com/sageserpent-open/plutonium/issues/57) exposed an infrequent bug via Scalacheck whose failing
test cases were frightfully complex and not shrinkable by default. Until that bug can be reproduced via a minimised test
case, it won't be fixed.

The problem is that the test cases have invariants that are constraints on how the test data is built up - simply
flinging arbitrary values of the right types together can build invalid test cases that cause such tests to fail with
false negatives independently of the system under test. What is needed is something that shrinks a failing test case
while sticking with the logic that enforces the invariant on the test cases being shrunk.

Working with Scalacheck on
this: [ImmutableObjectStorageSpec](https://github.com/sageserpent-open/curium/blob/8455ee0a387c6ab5373283a21f88ab6044d59ee1/src/test/scala/com/sageserpent/plutonium/curium/ImmutableObjectStorageSpec.scala#L227)
in the Curium project motivated the author to try out some alternatives to Scalacheck; Zio and Hedgehog were explored,
and they both allow integrated shrinking for free that respects the test case invariants. However they don't quite suit
the author's wishlist for parameterised testing in general, so along came Americium.

### The history starts with _"Start a project for the F# to Scala port of the Test Case Generation library."_. Huh? ###

Ah - a long time ago there was an F# project that used some utility code that was hived off into a helper assembly - see
here: https://github.com/sageserpent-open/NTestCaseBuilder/tree/master/development/solution/SageSerpent.Infrastructure

Some of that code was ported to Scala as a learning exercise and ended up being used by the Plutonium and Curium
projects as a shared dumping ground for utilities, including things for testing - see
here: https://github.com/sageserpent-open/americium/blob/master/src/main/scala/com/sageserpent/americium/RandomEnrichment.scala

Being lazy, the author carried on with the ignoble tradition of dumping experimental but useful code into the project,
then decided to bury its murky past and re-invent it as a respectable member of society supporting parameterised
testing. Now you know its terrible secret...

### Americium? Plutonium? Curium? ###

The author has a great appreciation of the actinide elements. There is a Neptunium project too, but he changed the name
of its repository to make its use-case more obvious.

### The introduction mentions Scalacheck Shapeless, do explain... ###

This the automatic generation of trials for structured types, and is brought to us via the Magnolia library - see it in
action here:
[TrialsSpec.scala](https://github.com/sageserpent-open/americium/blob/e69b9fb60cd90796d96ba1126a90f6c1ab2a7a1d/src/test/scala/com/sageserpent/americium/TrialsSpec.scala#L1057)
and here:
[TrialsSpec.scala](https://github.com/sageserpent-open/americium/blob/e69b9fb60cd90796d96ba1126a90f6c1ab2a7a1d/src/test/scala/com/sageserpent/americium/TrialsSpec.scala#L1067)

Ask for a trials of your case class hierarchy types, and it shall be written for you!

### When I use the `.choose` to build a trials instance from an API object, it won't shrink - why? ###

Yes, and that is currently by design. When you use the `.choose` method to build a trials instance, you are saying
that *you* want to provide the choices and that they are all equally as good - think of the members of an enumeration,
for instance, or perhaps some user input choices in a UI. The trials machinery doesn't know anything about the domain
these choices come from and won't try to order them according to some ranking of simplicity - they are taken to be
equally valid.

What *does* get shrunk is the complexity of the test cases - so if we have collections, or some kind of recursive
definition, then smaller collections are taken to be simpler, as are cases built with less recursion. Collections shrink
towards empty collections.

Furthermore, the streaming factory methods - `.doubles`, `.integers`, `.stream` etc also support shrinking - they have
an internal parameter that controls the range of the generated values, so as shrinkage proceeds, the values get '
smaller' in some sense. For numeric values, that usually means tending towards zero from both positive and negative
values.

The `.strings` factory method shrinks in the same manner as for collections - the strings get shorter, tending to the
empty string, although the characters range over the full UTF-16 set.

This choice isn't written in stone - rather than using `.choose`, use the streaming factory methods that allow custom
ranges, either via overloads of `.integers`, `.longs` and `.characters` or directly in `.stream` using a `CaseFactory`.
These also permit some other value to be the one that shrinkage tends to. See here for an
example: [TrialsApiTests](https://github.com/sageserpent-open/americium/blob/6fdd3db7f07e398018de80ce8130a5582648a346/src/test/scala/com/sageserpent/americium/java/TrialsApiTests.java#L309)
.

That example also shows how strings can be built from the output of `.characters` in Java - use this example
here: [TrialsSpec](https://github.com/sageserpent-open/americium/blob/6fdd3db7f07e398018de80ce8130a5582648a346/src/test/scala/com/sageserpent/americium/TrialsSpec.scala#L218)
if you are writing in Scala. Note that when you do this, you have the ability to shrink your strings based on both
length and the character values.

### Are the cases yielded by `.doubles`, `.integers`, `.stream` randomly distributed? ###

Yes, and they should span pretty much the whole range of allowed values. As shrinkage kicks in, this range contracts to
the 'minimal value' - zero for numeric values, but that can be customised when using `.stream`. See the `CaseFactory`
interface if you want to customise the range of allowed values and where the minimal value lies in that range, it
doesn't have to sit in the middle.

As mentioned in the previous section, there are also some convenience overloads of `.integers`, `.longs`
and `.characters` for this purpose too.

Hedgehog supports custom distributions and ranges, and Scalacheck has some heuristics for biasing its otherwise random
distributions. You can implement this by supplying your own `CaseFactory` instance that skews the input values, and you
can also move the input value for the maximally shrunk case to some favoured value, as shrinkage will home in on it.

### If I write a recursive definition of a trials instance, do I need to protect against infinite recursion with a size parameter? ###

No, but you do need to stop trivial infinite recursion. Thankfully that is simple, see here:
[TrialsSpec.scala](https://github.com/sageserpent-open/americium/blob/e69b9fb60cd90796d96ba1126a90f6c1ab2a7a1d/src/test/scala/com/sageserpent/americium/TrialsSpec.scala#L59)
Either wrap the recursive calls in a following bind in a flatmap, this will cause them to be evaluated lazily, or if you
need to lead with a recursive call, wrap it in a call to `.delay`. Both techniques are shown in that example.

Actually, I oversimplified - sure, you won't need to stop lazily-evaluated infinite recursion, but it is possible to
mess up a highly recursive trials instance so that it simply doesn't generate any data, due to what is called the '
complexity limit' kicking in. A potential case has an associated complexity, and if in the process of building up an
individual case the complexity exceeds the limit, then this will discard that case from being formulated - this is how
infinite recursion is prevented as a nice side benefit. However, one can write recursively formulated trials instances
that are 'bushy' - there are several parallel paths of recursion at each step, and this tends to result in complete and
utter failure to generate anything more than very simple cases. To see how this is worked around, take a look
here: [TrialsSpec.scala](https://github.com/sageserpent-open/americium/blob/5ea1b3088adaaa0270a944ee1694950975b2b911/src/test/scala/com/sageserpent/americium/TrialsSpec.scala#L94)
.

### I can see a reference to Scalacheck in the project dependencies. So is this just a sham? ###

Um ... you mean this one
here:  [TrialsLaws](https://github.com/sageserpent-open/americium/blob/afe7fca4215bfa00879b553aa7805bb5f8cf2d64/src/test/scala/com/sageserpent/americium/TrialsLaws.scala#L32)
?

Well, the Cats laws testing framework is used to prove that `Trials` is a decent, law-abiding monadic type, and that
framework plugs into Scalacheck, so yes, there is a *test* dependency.

The author is truly embarrassed by this, but in his defence, notes that if Cats laws testing could be plugged
into `Trials`, we would have recursively tested code. Not impossible to do, but requires a careful bootstrap approach to
avoid testing a broken SUT with itself.

An integration of `Trials` with Cats, or more generally with Scalacheck properties would be great though. Plenty of folk
like Scalacheck's properties, so while it's not to the author's taste, why exclude them?

### I want my test to work with multiple test parameters. ###

Right, you mean perhaps that you want to preconfigure an SUT as one test parameter and then have a test plan implemented
as a command sequence passed on via a second test parameter, or something like that? Maybe you have some stubs that
interact with the SUT that you want to preconfigure and pass in via their own test parameters?

Fear not - build up as many trials instances as you like for the bits and pieces you want to pass into the test, then
join them together with the `.and` method. You get back an object that you can call `.withLimit(...).supplyTo`
or `.withRecipe(...).supplyTo` as usual, only this time the test passed to `supplyTo` takes multiple parameters, or
alternatively tuples of parameters - your choice.

It's easy:
[Java example](https://github.com/sageserpent-open/americium/blob/afe7fca4215bfa00879b553aa7805bb5f8cf2d64/src/test/scala/com/sageserpent/americium/java/TrialsApiTests.java#L240)
,
[Scala example](https://github.com/sageserpent-open/americium/blob/afe7fca4215bfa00879b553aa7805bb5f8cf2d64/src/test/scala/com/sageserpent/americium/RichSeqSpec.scala#L50)
.

### Where are the controls? ###

Not down the sofa nestling betwixt the cushions - instead, they are to found between `Trials` and `.supplyTo` and are
called `.withLimit` (two overloads) and `.withLimits` (also two overloads).

The drill is to build your `Trials` instance to produce the right type of cases with any invariants you want, then to
call one of `.withLimit(s)` on it, thus configuring how the trials will supply cases to a following call of `.supplyTo`.

So the pattern is:

`<trials>.withLimit(s)(<configuration>).supplyTo(<test consumer>)`

For the simplest approach,
use [`.withLimit`](https://github.com/sageserpent-open/americium/blob/9b78c966f38773af3214adab524374af89cdd14b/src/main/scala/com/sageserpent/americium/java/TrialsScaffolding.java#L28)
and pass in the maximum number of cases you would like to have supplied to your test consumer.

In Scala, full bells and whistles is provided
by [`.withLimits`](https://github.com/sageserpent-open/americium/blob/9b78c966f38773af3214adab524374af89cdd14b/src/main/scala/com/sageserpent/americium/TrialsScaffolding.scala#L91)
.

This allows a maximum number of cases, the maximum complexity, the maximum number of shrinkage attempts and a 'shrinkage
stop' to be configured. Other than the mandatory maximum number of cases, all other items are optional.

In Java, the equivalent is provided by a combination
of [`.withLimits`](https://github.com/sageserpent-open/americium/blob/9b78c966f38773af3214adab524374af89cdd14b/src/main/scala/com/sageserpent/americium/java/TrialsScaffolding.java#L141)
and [`OptionalLimits`](https://github.com/sageserpent-open/americium/blob/9b78c966f38773af3214adab524374af89cdd14b/src/main/scala/com/sageserpent/americium/java/TrialsScaffolding.java#L83)
.

Setting the maximum number of shrinkage attempts to zero disables shrinkage altogether - so the original failing case is
yielded.

The shrinkage stop is a way for the user to control shrinkage externally via a callback style interface. Essentially
a [`ShrinkageStop`](https://github.com/sageserpent-open/americium/blob/9b78c966f38773af3214adab524374af89cdd14b/src/main/scala/com/sageserpent/americium/java/TrialsScaffolding.java#L65)
is a factory for a stateful predicate that you supply that could, say:

1. Monitor the number of invocations - thus counting the number of successful shrinkages.
2. Check heap usage.
3. Check against a timeout since the start of shrinkage.
4. Check the quality of the best shrunk case so far.

When it returns true, the shrinkage process is terminated early with the best shrunk case seen so far, regardless of
whether the maximum number of shrinkage attempts has been reached or not. In fact, there is a difference between
configuring the maximum number of shrinkage attempts and counting the shrinkages - the former includes a panic mode
where the shrinkage mechanism has not yet managed to shrink further on a previous failing case, but is still retrying,
whereas the shrinkage stop predicate is only invoked with freshly shrunk cases where progress has been made.

### Why is there a file 'IntelliJCodeStyle.xml' in the project? ###

The author has a real problem with pull requests that consist of a wholesale reformatting of sources that also harbour
some change in functionality within the reformatting noise. If you want to work on this and contribute back, please use
the automatic reformatting tools in IntelliJ (or similar) to stick to the existing style. Bear in mind that the chosen
formatting style isn't the author's favourite, but the simplicity of using automatic formatting on commit by far
outweighs the meagre joys of having the code look *just so*. Just wait until you do a merge...

The author uses IntelliJ's built-in Java formatter and the integration with Scalafmt.

If you know of a better way of sharing reformatting settings / tooling, raise an issue.

### Where is the Scaladoc? ###

Sorry, while the API has been in development the author has concentrated on keeping the Javadoc up to date. Hopefully
the Java and Scala APIs are similar enough to translate what is in the Javadoc that isn't captured in the Scala method
names and signatures. Someday, but until then pull requests are welcome...

### The competition? ###

In Scala, there is at least:

1. Scalacheck
2. ZioTest
3. Hedgehog
4. Scalaprops
5. Nyaya

In Java, there is at least:

1. Jqwik
2. QuickTheories
3. JUnit-QuickCheck
4. VavrTest
