package com.sageserpent.americium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NonEmptyCollectionsTest extends AnyFlatSpec with Matchers {
  val api = Trials.api

  val elementTrials = api.integers

  "nonEmptyCollections" should "yield only non-empty collections" in {
    elementTrials.nonEmptyCollections[List[Int]].withLimit(50).supplyTo {
      collection =>
        collection should not be empty
    }
  }

  "nonEmptyLists" should "yield only non-empty lists" in {
    elementTrials.nonEmptyLists.withLimit(50).supplyTo { collection =>
      collection should not be empty
    }
  }

  "nonEmptySets" should "yield only non-empty sets" in {
    elementTrials.nonEmptySets.withLimit(50).supplyTo { collection =>
      collection should not be empty
    }
  }

  "nonEmptySortedSets" should "yield only non-empty sorted sets" in {
    elementTrials.nonEmptySortedSets.withLimit(50).supplyTo { collection =>
      collection should not be empty
    }
  }

  "nonEmptyMaps" should "yield only non-empty maps" in {
    elementTrials.nonEmptyMaps(api.booleans).withLimit(50).supplyTo {
      collection =>
        collection should not be empty
    }
  }

  "nonEmptySortedMaps" should "yield only non-empty sorted maps" in {
    elementTrials.nonEmptySortedMaps(api.booleans).withLimit(50).supplyTo {
      collection =>
        collection should not be empty
    }
  }

  "nonEmptyStrings" should "yield only non-empty strings" in {
    api.nonEmptyStrings.withLimit(50).supplyTo { collection =>
      collection should not be empty
    }
  }
}
