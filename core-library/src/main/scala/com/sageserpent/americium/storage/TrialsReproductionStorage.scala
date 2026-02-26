package com.sageserpent.americium.storage

import cats.Eval
import com.sageserpent.americium.generation.JavaPropertyNames.{
  runDatabaseJavaProperty,
  temporaryDirectoryJavaProperty
}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.runDatabaseDefault
import com.sageserpent.americium.java.RecipeIsNotPresentException
import io.circe.parser.parse
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}

object TrialsReproductionStorage {
  val evaluation: Eval[TrialsReproductionStorage] =
    Eval.later {
      val result = new TrialsReproductionStorage(storagePath)
      Runtime.getRuntime.addShutdownHook(new Thread(() => result.close()))
      result
    }

  def readOnlyConnection(): TrialsReproductionStorage =
    new TrialsReproductionStorage(storagePath)

  private[storage] def storagePath: os.Path = {
    val tempDir = Option(System.getProperty(temporaryDirectoryJavaProperty))
      .getOrElse(
        throw new RuntimeException(
          s"No definition of Java property: `$temporaryDirectoryJavaProperty`"
        )
      )

    val databaseName = Option(System.getProperty(runDatabaseJavaProperty))
      .getOrElse(runDatabaseDefault)

    os.Path(tempDir) / s"$databaseName-trials"
  }

  private def filenameFor(recipeHash: String): String = {
    s"recipe-data-for-recipe-hash-$recipeHash.json"
  }

  // Case class for JSON serialization
  private[storage] case class RecipeData(
      recipe: String,
      structureOutline: String
  )

  private[storage] object RecipeData {
    implicit val encoder: Encoder[RecipeData] =
      io.circe.generic.semiauto.deriveEncoder
    implicit val decoder: Decoder[RecipeData] =
      io.circe.generic.semiauto.deriveDecoder
  }
}

class TrialsReproductionStorage(baseDir: os.Path) extends RecipeStorage {
  import TrialsReproductionStorage.*

  private val recipesDir = baseDir / "recipes"

  def recordRecipeHash(
      recipeHash: String,
      recipe: String,
      structureOutline: String
  ): Unit = {
    val data = RecipeData(recipe, structureOutline)
    val json = data.asJson.noSpaces

    atomicWrite(recipesDir / filenameFor(recipeHash), json)
  }

  def recipeFromRecipeHash(recipeHash: String): String = {
    val filePath = recipesDir / filenameFor(recipeHash)

    if (!os.exists(filePath)) {
      throw new RecipeIsNotPresentException(
        recipeHash,
        baseDir.toNIO
      )
    }

    val json = os.read(filePath)
    parseRecipeData(json, recipeHash).recipe
  }

  def structureOutlineFromRecipeHash(
      recipeHash: String
  ): Option[String] = {
    val filePath = recipesDir / filenameFor(recipeHash)

    if (!os.exists(filePath)) {
      None
    } else {
      val json = os.read(filePath)
      Some(parseRecipeData(json, recipeHash).structureOutline)
    }
  }

  override def reset(): Unit = {
    os.remove.all(recipesDir)
  }

  override def close(): Unit = {
    // File-based storage doesn't need explicit cleanup
    // OS will handle file handles
  }

  /** Atomically write content to a file using temp file + rename.
    *
    * This ensures that readers never see partial writes.
    */
  private def atomicWrite(path: os.Path, content: String): Unit = {
    // Write to temp file in same directory (ensures same filesystem)
    val tempPath = path / os.up / s".${path.last}.tmp"

    os.write.over(tempPath, content, createFolders = true)

    // Atomic move (rename is atomic on same filesystem)
    os.move(tempPath, path, replaceExisting = true)
  }

  private def parseRecipeData(json: String, recipeHash: String): RecipeData = {
    parse(json).flatMap(_.as[RecipeData]) match {
      case Right(data) => data
      case Left(error) =>
        throw new RuntimeException(
          s"Failed to parse recipe data for hash $recipeHash: ${error.getMessage}"
        )
    }
  }
}
