package com.sageserpent.americium.storage

import com.sageserpent.americium.java.RecipeIsNotPresentException
import com.sageserpent.americium.storage.TrialsReproductionStorage.RecipeData
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
          RecipeData(s"recipe$i", s"outline$i")
        )
      })
    }

    threads.foreach(_.start())
    threads.foreach(_.join())

    // Verify all 10 recipes present
    (1 to 10).foreach { i =>
      val RecipeData(recipe, structureOutline) =
        storage.recipeDataFromRecipeHash(s"hash$i")
      recipe shouldBe s"recipe$i"
      structureOutline shouldBe s"outline$i"
    }
  }

  it should "handle concurrent writes to same recipe atomically" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    for (_ <- 0 until 20) {
      @volatile var testFailure: Option[Throwable] = None

      val threads = (1 to 20).map { i =>
        new Thread(() => {
          try {
            storage.recordRecipeHash(
              "same-hash",
              RecipeData(s"recipe-$i", s"outline-$i")
            )
          } catch {
            case throwable: Throwable => testFailure = Some(throwable)
          }
        })
      }

      threads.foreach(_.start())
      threads.foreach(_.join())

      // Last writer wins - verify one valid recipe present
      val RecipeData(recipe, _) = storage.recipeDataFromRecipeHash("same-hash")
      recipe should startWith("recipe-")

      testFailure.foreach {
        case passThrough: TestFailedException => throw passThrough
        case throwable                        => fail(throwable)
      }
    }
  }

  it should "never expose partial writes to concurrent readers" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    // Pre-populate with initial recipe
    storage.recordRecipeHash(
      "test-hash",
      RecipeData("initial", "initial-outline")
    )

    @volatile var keepRunning: Boolean           = true
    @volatile var testFailure: Option[Throwable] = None

    // Writer thread - continuously updates
    val writer = new Thread(() => {
      var counter = 0
      while (keepRunning) {
        storage.recordRecipeHash(
          "test-hash",
          RecipeData(s"recipe-$counter", s"outline-$counter")
        )
        counter += 1
      }
    })

    // Reader thread - continuously reads
    val reader = new Thread(() => {
      while (keepRunning) {
        try {
          val RecipeData(recipe, structureOutline) =
            storage.recipeDataFromRecipeHash("test-hash")

          // Verify consistency: recipe and outline should match
          if (recipe.startsWith("recipe-")) {
            val num             = recipe.stripPrefix("recipe-")
            val expectedOutline = s"outline-$num"

            structureOutline should be(expectedOutline)
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
    storage.recordRecipeHash("hash", RecipeData("recipe", "outline"))

    os.exists(baseDir / "recipes") shouldBe true
  }

  it should "handle special characters in recipe hashes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    // Recipe hashes are typically hex, but test robustness
    val hash = "abc123-def456_xyz789"
    storage.recordRecipeHash(hash, RecipeData("recipe", "outline"))

    storage.recipeDataFromRecipeHash(hash).recipe shouldBe "recipe"
  }

  it should "throw RecipeIsNotPresentException for missing recipes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    an[RecipeIsNotPresentException] should be thrownBy {
      storage.recipeDataFromRecipeHash("nonexistent")
    }
  }

  it should "reset storage by clearing all recipes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    // Add some recipes
    storage.recordRecipeHash("hash1", RecipeData("recipe1", "outline1"))
    storage.recordRecipeHash("hash2", RecipeData("recipe2", "outline2"))

    // Reset
    storage.reset()

    // Verify all recipes are gone
    an[RecipeIsNotPresentException] should be thrownBy {
      storage.recipeDataFromRecipeHash("hash1")
    }
    an[RecipeIsNotPresentException] should be thrownBy {
      storage.recipeDataFromRecipeHash("hash2")
    }
  }

  it should "handle JSON special characters in recipes" in {
    val storage = new TrialsReproductionStorage(
      os.temp.dir(prefix = "americium-test-")
    )

    val recipeWithSpecialChars =
      """{"key":"value with \"quotes\" and \n newlines"}"""
    storage.recordRecipeHash(
      "hash",
      RecipeData(recipeWithSpecialChars, "outline")
    )

    storage
      .recipeDataFromRecipeHash("hash")
      .recipe shouldBe recipeWithSpecialChars
  }
}
