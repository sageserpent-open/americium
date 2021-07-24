# Americium - **_Test cases galore! Automatic case shrinkage! Bring your own test style. For Scala and Java..._** [![Build Status](https://travis-ci.com/sageserpent-open/americium.svg?branch=master)](https://travis-ci.com/sageserpent-open/americium)

## Why ##

You like writing parameterised tests - so you have a block of test code expressed as a test method or function of lambda
form, and something that pumps test cases into that code block as one or more arguments.

At this point, the likes of QuickCheck, FsCheck, Scalacheck and VavrTest come to mind, amongst others. If you are
working in Scala, then you'll probably be thinking of Scalacheck, maybe ZioTest, perhaps Hedgehog...? If in Java, then
JUnit-QuickCheck, or possibly VavrTest?

All great things - the author has the benefit of using Scalacheck for several years on various Scala works, finding all
kinds of obscure, knotty bugs that would otherwise lay hidden until the fateful day in production. Likewise VavrTest has
helped for the Java works. Fun has been had with ZioTest and Hedgehog too...

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

notSoStableSort(Nil: List[(Int, Int)])(Ordering.by(_._1)) // List() - Hey - worked first time...
notSoStableSort(List(1 -> 2))(Ordering.by(_._1)) // List((1,2)) - Yeah, check those edge cases!
notSoStableSort(List(1 -> 2, -1 -> 9))(Ordering.by(_._1)) // List((-1,9), (1,2)) - Fancy a beer, anyone?
notSoStableSort(List(1 -> 2, -1 -> 9, 1 -> 3))(Ordering.by(_._1)) // List((-1,9), (1,3), (1,2)) ? Uh? I wanted List((-1,9), (1,2), (1,3))!!!!
notSoStableSort(List(1 -> 2, 1 -> 3))(Ordering.by(_._1)) // List((1,3), (1,2)) ? Huh! I wanted List((1,2), (1,3)) - going to be working overtime...

```
Now this isn't so painful because it's a toy problem and we know exactly where to start debugging, and therefore how to minimise the test case (the last one is a minimal case, all we need to do is submit two entries that are not equal by `==` but are in terms of the supplied ordering).

Think this is always going to be the case? Take a look at this one (currently unsolved): https://github.com/sageserpent-open/plutonium/issues/57 - in particular, look at the test failure logs on the ticket. All that gibberish in the logs is *one single test case*. Want to try debugging through that? How would you minimise it?

This is made all the worse by the rarity of this bug - in fact, Scalacheck used to use random seed values back when this bug was first encountered, so the the test only failed once in a blue moon. To make this failure reproducible each time means that the test has to run a *long, long* time. Even more fun if you're in debugging session watching your breakpoints being hit for several hundred successful cases before you get to the one that finally fails, whichever it is...

What we want here is something that automatically shrinks a failing test case down to a minimal test case (or at least reasonably close to one), and provides some way of reproducing this minimal test case without having to slog through a whole bunch of successful cases we aren't interested in.

After toiling through quite a few of these monster test failures in the Plutonium, Curium and several commercial projects, the author decided to address this issue.

To be fair, there are some frameworks out there that also offer automatic test case shrinkage - your mileage may vary. Scalacheck does this, but with caveats: https://github.com/typelevel/scalacheck/pull/440. ZioTest does this too, give it a whirl and see how you fare. So does Hedgehog for that matter...

This brings us to the next pain point for the author, which is the extent to which the framework has opinions about how your code is to be structured. Scalacheck comes not only with generation of test cases, but is own property-checking DSL and style of assembling a test suite, which you may or may not buy into. There is an integration into Scalatest so that you can supply test cases to a Scalatest test - perhaps you might like that better? MUnit will let you use Scalacheck, but you are back to its own DSL ... or perhaps you'd prefer UTest - not sure what you'd do there...

... or maybe you write in Java and use JUnit? What then? VavrTest doesn't at time of writing offer any shrinkage support.

What the author wanted was a framework that:

1. Offers automatic shrinkage to a minimal or nearly-minimal test case.
1. Offers direct reproduction of a failing, minimised test case.
1. Gets out of the way of testing style - doesn't care about whether the test are pure functional or imperative, doesn't offer a DSL or try to structure your test suite.
1. Supports Scala and Java as first class citizens.
1. Supports covariance of test case generation in Scala, so cases for a subclass can be substituted for cases for a supertrait/superclass.
1. Supports covariance of test case generation in Java, so cases for a subclass can be substituted for cases for a superinterface/superclass.
1. Allows automatic derivation of test case generation for sum/product types (aka case class hierarchies) in the spirit of Scalacheck Shapeless.

So finally, we come to `Trials` - which is what this library has to offer. 

Well, that and some syntax enhancements to the Scala `Random` class that might also pique your interest, but go see for yourself in the code, it's simple enough...


## What ##

## Where ##

## Rhetorical Questions ##

### How did this come about? ###