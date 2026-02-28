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

class TrialsReproductionStorage(baseDir: os.Path) extends FileBasedStorage {
  import TrialsReproductionStorage.*

  override protected val storageDirectory: os.Path = baseDir / "recipes"

  def recordRecipeHash(
      recipeHash: String,
      recipeData: RecipeData
  ): Unit = {
    val json = recipeData.asJson.noSpaces
    atomicWrite(storageDirectory / filenameFor(recipeHash), json)
  }

  def recipeDataFromRecipeHash(recipeHash: String): RecipeData = {
    val filePath = storageDirectory / filenameFor(recipeHash)

    try {
      val json = atomicRead(filePath)

      parse(json).flatMap(_.as[RecipeData]) match {
        case Right(data) => data
        case Left(error) =>
          throw new RuntimeException(
            s"Failed to parse recipe data for hash $recipeHash: ${error.getMessage}"
          )
      }
    } catch {
      case _: java.nio.file.NoSuchFileException =>
        throw new RecipeIsNotPresentException(recipeHash, storageDirectory)
    }
  }
}
