package com.sageserpent.americium.storage

import cats.Eval
import com.sageserpent.americium.generation.JavaPropertyNames.{
  runDatabaseJavaProperty,
  temporaryDirectoryJavaProperty
}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.runDatabaseDefault
import com.sageserpent.americium.java.RecipeIsNotPresentException
import io.circe.generic.auto.*
import io.circe.parser.parse
import io.circe.syntax.*

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

  case class RecipeData(
      recipe: String,
      structureOutline: String
  )
}

class TrialsReproductionStorage(baseDir: os.Path) extends RecipeStorage {
  import TrialsReproductionStorage.*

  private val recipesDir = baseDir / "recipes"

  def recordRecipeHash(
      recipeHash: String,
      recipeData: RecipeData
  ): Unit = {
    val json = recipeData.asJson.noSpaces

    atomicWrite(recipesDir / filenameFor(recipeHash), json)
  }

  /** Atomically write content to a file using temp file + rename.
    *
    * This ensures that readers never see partial writes.
    */
  private def atomicWrite(path: os.Path, content: String): Unit = {
    // Write to temp file in same directory (ensures same filesystem)
    val threadSpecificNameToAvoidContention =
      s".${path.last}.${Thread.currentThread().getId}.tmp"
    val tempPath = recipesDir / threadSpecificNameToAvoidContention

    os.write.over(tempPath, content, createFolders = true)
    // Atomic move (rename is atomic on same filesystem)
    os.move(tempPath, path, replaceExisting = true)
  }

  def recipeDataFromRecipeHash(recipeHash: String): RecipeData = {
    val filePath = recipesDir / filenameFor(recipeHash)

    if (!os.exists(filePath)) {
      throw new RecipeIsNotPresentException(recipeHash, recipesDir)
    }

    val json = os.read(filePath)

    parse(json).flatMap(_.as[RecipeData]) match {
      case Right(data) => data
      case Left(error) =>
        throw new RuntimeException(
          s"Failed to parse recipe data for hash $recipeHash: ${error.getMessage}"
        )
    }
  }

  override def reset(): Unit = {
    os.remove.all(recipesDir)
  }

  override def close(): Unit = {
    // File-based storage doesn't need explicit cleanup
    // OS will handle file handles
  }
}
