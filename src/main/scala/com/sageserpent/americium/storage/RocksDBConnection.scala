package com.sageserpent.americium.storage
import cats.Eval
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.generation.JavaPropertyNames.{runDatabaseJavaProperty, temporaryDirectoryJavaProperty}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.runDatabaseDefault
import org.rocksdb.*

import _root_.java.util.ArrayList as JavaArrayList
import java.nio.file.Path

object RocksDBConnection {
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

  private def connection(readOnly: Boolean): RocksDBConnection = {
    Option(System.getProperty(temporaryDirectoryJavaProperty)).fold(ifEmpty =
      throw new RuntimeException(
        s"No definition of Java property: `$temporaryDirectoryJavaProperty`"
      )
    ) { directory =>
      val runDatabase = Option(
        System.getProperty(runDatabaseJavaProperty)
      ).getOrElse(runDatabaseDefault)

      val columnFamilyDescriptors =
        ImmutableList.of(
          defaultColumnFamilyDescriptor,
          columnFamilyDescriptorForRecipeHashes,
          columnFamilyDescriptorForTestCaseIds
        )

      val columnFamilyHandles = new JavaArrayList[ColumnFamilyHandle]()

      val rocksDB =
        if (readOnly)
          RocksDB.openReadOnly(
            rocksDbOptions,
            Path
              .of(directory)
              .resolve(runDatabase)
              .toString,
            columnFamilyDescriptors,
            columnFamilyHandles
          )
        else
          RocksDB.open(
            rocksDbOptions,
            Path
              .of(directory)
              .resolve(runDatabase)
              .toString,
            columnFamilyDescriptors,
            columnFamilyHandles
          )

      RocksDBConnection(
        rocksDB,
        columnFamilyHandleForRecipeHashes = columnFamilyHandles.get(1),
        columnFamilyHandleForTestCaseIds = columnFamilyHandles.get(2)
      )
    }
  }

  def readOnlyConnection(): RocksDBConnection = connection(readOnly = true)

  val evaluation: Eval[RocksDBConnection] =
    Eval.later {
      val result = connection(readOnly = false)
      Runtime.getRuntime.addShutdownHook(new Thread(() => result.close()))
      result
    }
}

// TODO: split the responsibilities into two databases? `SupplyToSyntaxSkeletalImplementation` cares about recipe
// hashes and is core functionality, whereas `TrialsTestExtension` cares about test case ids and is an add-on.
case class RocksDBConnection(
    rocksDb: RocksDB,
    columnFamilyHandleForRecipeHashes: ColumnFamilyHandle,
    columnFamilyHandleForTestCaseIds: ColumnFamilyHandle
) {
  def recordRecipeHash(recipeHash: String, recipe: String): Unit = {
    rocksDb.put(
      columnFamilyHandleForRecipeHashes,
      recipeHash.map(_.toByte).toArray,
      recipe.map(_.toByte).toArray
    )
  }

  // TODO: suppose there isn't a recipe? This should look like
  // `recipeFromTestCaseId`...
  def recipeFromRecipeHash(recipeHash: String): String = rocksDb
    .get(
      columnFamilyHandleForRecipeHashes,
      recipeHash.map(_.toByte).toArray
    )
    .map(_.toChar)
    .mkString

  def recordTestCaseId(testCaseId: String, recipe: String): Unit = {
    rocksDb.put(
      columnFamilyHandleForTestCaseIds,
      testCaseId.map(_.toByte).toArray,
      recipe.map(_.toByte).toArray
    )
  }

  def recipeFromTestCaseId(testCaseId: String): Option[String] = Option(
    rocksDb
      .get(
        columnFamilyHandleForTestCaseIds,
        testCaseId.map(_.toByte).toArray
      )
  ).map(_.map(_.toChar).mkString)

  def close(): Unit = {
    columnFamilyHandleForRecipeHashes.close()
    columnFamilyHandleForTestCaseIds.close()
    rocksDb.close()
  }
}
