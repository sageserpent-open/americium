package com.sageserpent.americium.storage

import com.sageserpent.americium.java.RecipeIsNotPresentException
import org.scalatest.exceptions.TestFailedException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TrialsReproductionStorageSpec extends AnyFlatSpec with Matchers {

  behavior of "TrialsReproductionStorage with concurrent writes"

  it should "handle concurrent writes to different recipes without conflicts" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    val threads = (1 to 20).map { i =>
      new Thread(() => {
        storage.recordRecipeHash(
          s"hash$i",
          s"recipe$i",
          s"outline$i"
        )
      })
    }

    threads.foreach(_.start())
    threads.foreach(_.join())

    // Verify all 10 recipes present
    (1 to 10).foreach { i =>
      storage.recipeFromRecipeHash(s"hash$i") shouldBe s"recipe$i"
      storage.structureOutlineFromRecipeHash(s"hash$i") shouldBe Some(
        s"outline$i"
      )
    }
  }

  it should "handle concurrent writes to same recipe atomically" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    for (_ <- 0 until 20) {
      val threads = (1 to 20).map { i =>
        new Thread(() => {
          storage.recordRecipeHash(
            "same-hash",
            s"recipe-$i",
            s"outline-$i"
          )
        })
      }

      threads.foreach(_.start())
      threads.foreach(_.join())

      // Last writer wins - verify one valid recipe present
      val recipe = storage.recipeFromRecipeHash("same-hash")
      recipe should startWith("recipe-")

      // Verify file is well-formed (not corrupted)
      noException should be thrownBy {
        storage.structureOutlineFromRecipeHash("same-hash")
      }
    }
  }

  it should "never expose partial writes to concurrent readers" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    // Pre-populate with initial recipe
    storage.recordRecipeHash("test-hash", "initial", "initial-outline")

    @volatile var keepRunning: Boolean           = true
    @volatile var testFailure: Option[Throwable] = None

    // Writer thread - continuously updates
    val writer = new Thread(() => {
      var counter = 0
      while (keepRunning) {
        storage.recordRecipeHash(
          "test-hash",
          s"recipe-$counter",
          s"outline-$counter"
        )
        counter += 1
      }
    })

    // Reader thread - continuously reads
    val reader = new Thread(() => {
      while (keepRunning) {
        try {
          val recipe  = storage.recipeFromRecipeHash("test-hash")
          val outline = storage.structureOutlineFromRecipeHash("test-hash")

          // Verify consistency: recipe and outline should match
          if (recipe.startsWith("recipe-")) {
            val num             = recipe.stripPrefix("recipe-")
            val expectedOutline = s"outline-$num"

            outline should be(Some(expectedOutline))
          }
        } catch {
          case throwable: Throwable => testFailure = Some(throwable)
        }
      }
    })

    writer.start()
    reader.start()

    Thread.sleep(1000) // Run for 1 second

    keepRunning = false
    writer.join()
    reader.join()

    testFailure.foreach {
      case passThrough: TestFailedException => throw passThrough
      case throwable                        => fail(throwable)
    }
  }

  behavior of "TrialsReproductionStorage with file operations"

  it should "create directories if they don't exist" in {
    val baseDir = os.temp.dir(prefix = "americium-test-")
    os.remove.all(baseDir) // Ensure it doesn't exist

    val storage = new TrialsReproductionStorage(baseDir)
    storage.recordRecipeHash("hash", "recipe", "outline")

    os.exists(baseDir / "recipes") shouldBe true
  }

  it should "handle special characters in recipe hashes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    // Recipe hashes are typically hex, but test robustness
    val hash = "abc123-def456_xyz789"
    storage.recordRecipeHash(hash, "recipe", "outline")

    storage.recipeFromRecipeHash(hash) shouldBe "recipe"
  }

  it should "throw RecipeIsNotPresentException for missing recipes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    an[RecipeIsNotPresentException] should be thrownBy {
      storage.recipeFromRecipeHash("nonexistent")
    }
  }

  it should "return None for missing structure outlines" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    storage.structureOutlineFromRecipeHash("nonexistent") shouldBe None
  }

  it should "reset storage by clearing all recipes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    // Add some recipes
    storage.recordRecipeHash("hash1", "recipe1", "outline1")
    storage.recordRecipeHash("hash2", "recipe2", "outline2")

    // Reset
    storage.reset()

    // Verify all recipes are gone
    an[RecipeIsNotPresentException] should be thrownBy {
      storage.recipeFromRecipeHash("hash1")
    }
    an[RecipeIsNotPresentException] should be thrownBy {
      storage.recipeFromRecipeHash("hash2")
    }
  }

  it should "handle JSON special characters in recipes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    val recipeWithSpecialChars =
      """{"key":"value with \"quotes\" and \n newlines"}"""
    storage.recordRecipeHash("hash", recipeWithSpecialChars, "outline")

    storage.recipeFromRecipeHash("hash") shouldBe recipeWithSpecialChars
  }
}
