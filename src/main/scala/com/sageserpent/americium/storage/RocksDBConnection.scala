package com.sageserpent.americium.storage
import cats.Eval
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.generation.JavaPropertyNames.{
  runDatabaseJavaProperty,
  temporaryDirectoryJavaProperty
}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.runDatabaseDefault
import com.sageserpent.americium.java.RecipeIsNotPresentException
import com.sageserpent.americium.storage.RocksDBConnection.databasePath
import org.rocksdb.*

import _root_.java.util.ArrayList as JavaArrayList
import java.nio.file.Path
import scala.util.Using

object RocksDBConnection {

  val evaluation: Eval[RocksDBConnection] =
    Eval.later {
      val result = connection(readOnly = false)
      Runtime.getRuntime.addShutdownHook(new Thread(() => result.close()))
      result
    }
  private val rocksDbOptions = new DBOptions()
    .optimizeForSmallDb()
    .setCreateIfMissing(true)
    .setCreateMissingColumnFamilies(true)
  private val columnFamilyOptions = new ColumnFamilyOptions()
    .setCompressionType(CompressionType.LZ4_COMPRESSION)
    .setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION)
  private val defaultColumnFamilyDescriptor = new ColumnFamilyDescriptor(
    RocksDB.DEFAULT_COLUMN_FAMILY,
    columnFamilyOptions
  )
  private val columnFamilyDescriptorForRecipeHashes =
    new ColumnFamilyDescriptor(
      "RecipeHashKeyRecipeValue".getBytes(),
      columnFamilyOptions
    )
  private val columnFamilyDescriptorForTestCaseIds = new ColumnFamilyDescriptor(
    "TestCaseIdKeyRecipeValue".getBytes(),
    columnFamilyOptions
  )
  private val columnFamilyDescriptorForGenerationMetadata =
    new ColumnFamilyDescriptor(
      "RecipeHashKeyGenerationMetadata".getBytes(),
      columnFamilyOptions
    )

  def readOnlyConnection(): RocksDBConnection = connection(readOnly = true)

  private def connection(readOnly: Boolean): RocksDBConnection = {
    val columnFamilyDescriptors =
      ImmutableList.of(
        defaultColumnFamilyDescriptor,
        columnFamilyDescriptorForRecipeHashes,
        columnFamilyDescriptorForTestCaseIds,
        columnFamilyDescriptorForGenerationMetadata
      )

    val columnFamilyHandles = new JavaArrayList[ColumnFamilyHandle]()

    val rocksDB =
      if (readOnly)
        RocksDB.openReadOnly(
          rocksDbOptions,
          databasePath.toString,
          columnFamilyDescriptors,
          columnFamilyHandles
        )
      else
        RocksDB.open(
          rocksDbOptions,
          databasePath.toString,
          columnFamilyDescriptors,
          columnFamilyHandles
        )

    RocksDBConnection(
      rocksDB,
      columnFamilyHandleForRecipeHashes = columnFamilyHandles.get(1),
      columnFamilyHandleForTestCaseIds = columnFamilyHandles.get(2),
      columnFamilyHandleForGenerationMetadata = columnFamilyHandles.get(3)
    )
  }

  private def databasePath: Path =
    Option(System.getProperty(temporaryDirectoryJavaProperty)) match {
      case None =>
        throw new RuntimeException(
          s"No definition of Java property: `$temporaryDirectoryJavaProperty`"
        )

      case Some(directory) =>
        val file = Option(
          System.getProperty(runDatabaseJavaProperty)
        ).getOrElse(runDatabaseDefault)

        Path
          .of(directory)
          .resolve(file)
    }
}

// TODO: split the responsibilities into two databases? `SupplyToSyntaxSkeletalImplementation` cares about recipe
// hashes and is core functionality, whereas `TrialsTestExtension` cares about `UniqueId` and is an add-on.
case class RocksDBConnection(
    rocksDb: RocksDB,
    columnFamilyHandleForRecipeHashes: ColumnFamilyHandle,
    columnFamilyHandleForTestCaseIds: ColumnFamilyHandle,
    columnFamilyHandleForGenerationMetadata: ColumnFamilyHandle
) extends AutoCloseable {
  def reset(): Unit = {
    dropColumnFamilyEntries(columnFamilyHandleForRecipeHashes)
    dropColumnFamilyEntries(columnFamilyHandleForTestCaseIds)
    dropColumnFamilyEntries(columnFamilyHandleForGenerationMetadata)
  }

  private def dropColumnFamilyEntries(
      columnFamilyHandle: ColumnFamilyHandle
  ): Unit =
    Using.resource(rocksDb.newIterator(columnFamilyHandle)) { iterator =>
      val firstRecipeHash: Array[Byte] = {
        iterator.seekToFirst()
        iterator.key
      }

      val onePastLastRecipeHash: Array[Byte] = {
        iterator.seekToLast()
        iterator.key() :+ 0
      }

      // NOTE: the range has an exclusive upper bound, hence the use of
      // `onePastLastRecipeHash`.
      rocksDb.deleteRange(
        columnFamilyHandle,
        firstRecipeHash,
        onePastLastRecipeHash
      )

    }

  def recordRecipeHash(
      recipeHash: String,
      recipe: String,
      structureOutline: String
  ): Unit = {
    // Store the recipe.
    rocksDb.put(
      columnFamilyHandleForRecipeHashes,
      recipeHash.map(_.toByte).toArray,
      recipe.map(_.toByte).toArray
    )

    // Store the structural outline.
    rocksDb.put(
      columnFamilyHandleForGenerationMetadata,
      recipeHash.map(_.toByte).toArray,
      structureOutline.map(_.toByte).toArray
    )
  }

  def recipeFromRecipeHash(recipeHash: String): String = Option(
    rocksDb
      .get(
        columnFamilyHandleForRecipeHashes,
        recipeHash.map(_.toByte).toArray
      )
  ) match {
    case Some(value) => value.map(_.toChar).mkString
    case None => throw new RecipeIsNotPresentException(recipeHash, databasePath)
  }

  def structureOutlineFromRecipeHash(
      recipeHash: String
  ): Option[String] = {
    Option(
      rocksDb.get(
        columnFamilyHandleForGenerationMetadata,
        recipeHash.map(_.toByte).toArray
      )
    ).map(_.map(_.toChar).mkString)
  }

  // TODO: shouldn't `uniqueId` be typed as `UniqueId`!
  def recordUniqueId(uniqueId: String, recipe: String): Unit = {
    rocksDb.put(
      columnFamilyHandleForTestCaseIds,
      uniqueId.map(_.toByte).toArray,
      recipe.map(_.toByte).toArray
    )
  }

  // TODO: shouldn't `uniqueId` be typed as `UniqueId`! Also, why does this get
  // to quietly wrap a null result into an `Option`, whereas
  // `recipeFromRecipeHash` throws an exception?
  def recipeFromUniqueId(uniqueId: String): Option[String] = Option(
    rocksDb
      .get(
        columnFamilyHandleForTestCaseIds,
        uniqueId.map(_.toByte).toArray
      )
  ).map(_.map(_.toChar).mkString)

  def close(): Unit = {
    columnFamilyHandleForRecipeHashes.close()
    columnFamilyHandleForTestCaseIds.close()
    columnFamilyHandleForGenerationMetadata.close()
    rocksDb.close()
  }
}
