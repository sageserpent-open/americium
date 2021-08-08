# Americium - **_Test cases galore! Automatic case shrinkage! Bring your own test style. For Scala and

Java..._** [![Build Status](https://travis-ci.com/sageserpent-open/americium.svg?branch=master)](https://travis-ci.com/sageserpent-open/americium)

## Why ##

You like writing parameterised tests - so you have a block of test code expressed as a test method or function of lambda
form, and something that pumps test cases into that code block as one or more arguments.

At this point, the likes of QuickCheck, FsCheck, Scalacheck and VavrTest come to mind, amongst others. If you are
working in Scala, then you'll probably be thinking of Scalacheck, maybe ZioTest, perhaps Hedgehog...? If in Java, then
JUnit-QuickCheck, or possibly VavrTest?

All great things - the author has had the benefit of using Scalacheck for several years on various Scala works, finding
all kinds of obscure, knotty bugs that would otherwise lay hidden until the fateful day in production. Likewise VavrTest
has helped for the Java works. Fun has been had with ZioTest and Hedgehog too...

However, one nagging problem with both Scalacheck and VavrTest is in the matter of test case shrinkage - it's all very
well when a parameterised test fails for some case, but reproducing and debugging the failure can be a real pain.

For one thing, not all frameworks allow direct reproduction of the offending test case - so if each individual test
execution for a piece of data takes appreciable time, then running the entire parameterised test up to the point of
failure can take minutes for more sophisticated tests. What's more, the test case that provokes the test failure may be
extraordinarily complex; these frameworks all use the notion of building up test cases based on combining randomly
varying data into bigger and bigger chunks, which often means that whtever provokes a failure is buried in a complex
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
your code is to be structured. Scalacheck comes not only with generation of test cases, but is own property-checking DSL
and style of assembling a test suite, which you may or may not buy into. There is an integration into Scalatest so that
you can supply test cases to a Scalatest test - perhaps you might like that better? MUnit will let you use Scalacheck,
but you are back to its own DSL ... or perhaps you'd prefer UTest - not sure what you'd do there...

... or maybe you write in Java and use JUnit? What then? VavrTest doesn't at time of writing offer any shrinkage
support.

What the author wanted was a framework that:

1. Offers automatic shrinkage to a minimal or nearly-minimal test case.
1. Shrinks efficiently.
1. Offers direct reproduction of a failing, minimised test case.
1. Covers finite combinations of atomic cases without duplication when building composite cases.
1. Gets out of the way of testing style - doesn't care about whether the test are pure functional or imperative, doesn't
   offer a DSL or try to structure your test suite.
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

## How ##

Let's take our sorting implementation above, write some proper parameterised tests and drive them via a `Trials`
instance ...

```scala
// We're going to sort a list of associations (key-value pairs) by the key...
val ordering = Ordering.by[(Int, Int), Int](_._1)

val api = Trials.api

// Here's the trials instance...
val associationLists = (for {
  key <- api.choose(0 to 100)
  value <- api.integers
} yield key -> value).lists

"stableSorting" should "sort according to the ordering" in
  associationLists
    .filter(
      _.nonEmpty
    ) // Filter out the empty case as we can't assert sensibly on it.
    .withLimit(200)
    .supplyTo { nonEmptyAssocationList: List[(Int, Int)] =>
      val sortedResult = notSoStableSort(nonEmptyAssocationList)(ordering)

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

```org.scalatest.exceptions.TestFailedException: HashMap(97 -> List((97,1809838260), (97,-1532594126))) did not equal HashMap(97 -> List((97,-1532594126), (97,1809838260)))
Expected :HashMap(97 -> List((97,-1532594126), (97,1809838260)))
Actual   :org.scalatest.exceptions.TestFailedException: HashMap(97 -> List((97,1809838260), (97,-1532594126)))

Trial exception with underlying cause:
org.scalatest.exceptions.TestFailedException: HashMap(97 -> List((97,1809838260), (97,-1532594126))) did not equal HashMap(97 -> List((97,-1532594126), (97,1809838260)))
Case:
List((97,-1532594126), (97,1809838260))
```

We also see a recipe for reproduction too in the output. We can use this recipe to make a temporary bug-reproduction
test that focuses solely on the test case causing the problem:

```scala
// Until the bug is fixed, we expect this test to fail...
it should "also preserve the original order of the subsequences of elements that are equivalent according to the order - this time with the failure reproduced directly" in
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
        |            "index" : 97
        |        }
        |    },
        |    {
        |        "FactoryInputOf" : {
        |            "input" : 1358603204065315550
        |        }
        |    },
        |    {
        |        "ChoiceOf" : {
        |            "index" : 1
        |        }
        |    },
        |    {
        |        "ChoiceOf" : {
        |            "index" : 97
        |        }
        |    },
        |    {
        |        "FactoryInputOf" : {
        |            "input" : -84795172735105265
        |        }
        |    },
        |    {
        |        "ChoiceOf" : {
        |            "index" : 0
        |        }
        |    }
        |]""".stripMargin)
    .supplyTo { associationList: List[(Int, Int)] => {
      val sortedResult = notSoStableSort(associationList)(ordering)

      assert(sortedResult.groupBy(_._1) == associationList.groupBy(_._1))
    }
    }  
```

## Where ##

## Rhetorical Questions ##

### How did this come about? ###