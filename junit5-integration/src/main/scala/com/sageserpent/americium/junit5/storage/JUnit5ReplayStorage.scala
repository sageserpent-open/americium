package com.sageserpent.americium.junit5.storage

import cats.Eval
import com.google.common.hash.Hashing as GuavaHashing
import com.sageserpent.americium.generation.JavaPropertyNames.{
  runDatabaseJavaProperty,
  temporaryDirectoryJavaProperty
}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.runDatabaseDefault
import com.sageserpent.americium.junit5.storage.JUnit5ReplayStorage.filenameFor
import com.sageserpent.americium.storage.RecipeStorage

object JUnit5ReplayStorage {
  val evaluation: Eval[JUnit5ReplayStorage] =
    Eval.later {
      val result = new JUnit5ReplayStorage(storagePath)
      Runtime.getRuntime.addShutdownHook(new Thread(() => result.close()))
      result
    }

  private[storage] def storagePath: os.Path = {
    val tempDir = Option(System.getProperty(temporaryDirectoryJavaProperty))
      .getOrElse(
        throw new RuntimeException(
          s"No definition of Java property: `$temporaryDirectoryJavaProperty`"
        )
      )

    val databaseName = Option(System.getProperty(runDatabaseJavaProperty))
      .getOrElse(runDatabaseDefault)

    os.Path(tempDir) / s"$databaseName-junit5"
  }

  private def filenameFor(uniqueId: String): String = {
    // Use a hash, because unique ids are full of interesting characters that
    // can't belong to filenames and also get far too long for OS limits.
    val hash = GuavaHashing
      .murmur3_128()
      .hashUnencodedChars(uniqueId)
      .toString

    s"recipe-for-unique-id-hash-$hash.txt"
  }
}

class JUnit5ReplayStorage(baseDir: os.Path) extends RecipeStorage {

  private val replayDir = baseDir / "junit5-replay"

  def recordUniqueId(uniqueId: String, recipe: String): Unit = {
    atomicWrite(replayDir / filenameFor(uniqueId), recipe)
  }

  /** Atomically write content to a file using temp file + rename. */
  private def atomicWrite(path: os.Path, content: String): Unit = {
    val tempPath = path / os.up / s".${path.last}.tmp"
    os.write.over(tempPath, content, createFolders = true)
    os.move(tempPath, path, replaceExisting = true)
  }

  def recipeFromUniqueId(uniqueId: String): Option[String] = {
    val filePath = replayDir / filenameFor(uniqueId)

    if (os.exists(filePath)) {
      Some(os.read(filePath))
    } else {
      None
    }
  }

  override def reset(): Unit = {
    if (os.exists(replayDir)) {
      os.list(replayDir)
        .filter(_.ext == "txt")
        .foreach(os.remove)
    }
  }

  override def close(): Unit = {
    // File-based storage doesn't need explicit cleanup
  }
}
