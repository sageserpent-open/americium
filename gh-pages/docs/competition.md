---
layout: default
title: "The competition"
parent: Wiki Content
nav_order: 11
---

# The competition
{: .no_toc }

Oh, that bunch?
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Americium fits in a wider ecosystem of property based testing tools.

A lot of these are based on a Haskell implementation, QuickCheck, and there are some others that take their own approaches, including Americium itself.

## Java

1. [Jqwik](https://jqwik.net/) - The author [Johannes Link](https://github.com/jlink) maintains a nice challenge repository here: [The Shrinking Challenge](https://github.com/jlink/shrinking-challenge) that both Jqwik and Americium have Java submissions to, along with other tools for other languages. Jqwik is a powerful tool, making heavy use of annotations and is intended solely for use with JUnit5 framework, using its own engine instead of the Jupiter one. Like Americium, shrinking is integrated, so comes for free. The documentation is very comprehensive. Like Americium, it is agnostic about the assertion language, but is more prescriptive about test structure.

1. [Quick Theories](https://github.com/quicktheories/QuickTheories) - not in the shrinking challenge, but again, shrinking is integrated. It uses a DSL based approach like Americium, and allows breakout from its own assertion language to using others. There is no integration with JUnit5 other than simply embedding a 'theory' into a JUnit test - this looks rather like using Americium's `.supplyTo` within a conventional `@Test` JUnt5 test.

1. [JUnit-Quickcheck](http://pholser.github.io/junit-quickcheck/site/1.0/) - not in the shrinking challenge, and follows the approach of QuickCheck, so you have to write your own shrinkage helpers if your test cases are of custom types. It integrates with JUnit5 using the Jupiter engine, and makes use of annotations.

1. [Vavr Test](https://docs.vavr.io/#_property_checking) - does not support shrinking. The website documentation is very sparse - go look at the code (which has good Javadoc). It is a port of the core concepts of Scalacheck to Java, using the Vavr framework, even leaner and meaner than Americium!

## Scala

1.  [Scalacheck](https://scalacheck.org/) The incumbent tool in the Scala world. Can be used standalone or integrated with Scalatest. Documentation is fairly good, covering the basics; the assumption is that if you use Scala, most of the monadic DSL will be second nature. Not in the shrinking challenge, and as this follows the QuickCheck approach, you have to write your own shrinkage helpers if your test cases are of custom types - the defaults ones can break and are disabled by default these days. Has its own assertion language, but the Scalatest integration uses Scalatest assertions instead.

1. [Hedgehog](https://hedgehogqa.github.io/scala-hedgehog/) Lean and mean DSL approach, with integrated shrinking and its own assertion language - not in the shrinking challenge. Documentation is good. Has integrations with Minitest and MUnit.

1. [Scalaprops](https://github.com/scalaprops/scalaprops)

1. [Nyaya](https://github.com/japgolly/nyaya)

1. [ZioTest](https://zio.dev/reference/test/property-testing/)

There are several other such tools in the aforementioned shrinking challenge, including [Hypothesis](https://github.com/HypothesisWorks/hypothesis) for Python folk, so if you came here but had another language in mind, here's the link again: [The Shrinking Challenge](https://github.com/jlink/shrinking-challenge).


***
Next topic: [Arrived from Scalacheck?...]({% link docs/wiki-content/from-scalacheck.md %})