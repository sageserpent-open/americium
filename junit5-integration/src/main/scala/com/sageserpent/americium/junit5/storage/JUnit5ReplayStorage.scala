package com.sageserpent.americium.junit5.storage

import cats.Eval
import com.google.common.hash.Hashing as GuavaHashing
import com.sageserpent.americium.generation.JavaPropertyNames.{
  runDatabaseJavaProperty,
  temporaryDirectoryJavaProperty
}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.runDatabaseDefault
import com.sageserpent.americium.storage.FileBasedStorage

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
}

class JUnit5ReplayStorage(baseDir: os.Path) extends FileBasedStorage {

  type Key = String

  override protected val storageDirectory: os.Path = baseDir / "junit5-replay"

  def recordUniqueId(uniqueId: Key, recipe: String): Unit = {
    atomicWrite(uniqueId, recipe)
  }

  def recipeFromUniqueId(uniqueId: Key): Option[String] = {
    try {
      Some(atomicRead(uniqueId))
    } catch {
      case _: java.nio.file.NoSuchFileException => None
    }
  }

  def filenameFor(uniqueId: Key): String = {
    // Use a hash, because unique ids are full of interesting characters that
    // can't belong to filenames and also get far too long for OS limits.
    val hash = GuavaHashing
      .murmur3_128()
      .hashUnencodedChars(uniqueId)
      .toString

    s"recipe-for-unique-id-hash-$hash.txt"
  }
}
