package com.sageserpent.americium.storage

import cats.Eval
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.generation.JavaPropertyNames.{
  runDatabaseJavaProperty,
  temporaryDirectoryJavaProperty
}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.runDatabaseDefault
import org.rocksdb.*

import _root_.java.util.ArrayList as JavaArrayList
import java.nio.file.Path
import scala.util.Using

object JUnit5ReplayDatabase {
  val evaluation: Eval[JUnit5ReplayDatabase] =
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

  private val columnFamilyDescriptorForRecipes = new ColumnFamilyDescriptor(
    "TestCaseIdKeyRecipeValue".getBytes(),
    columnFamilyOptions
  )

  def readOnlyConnection(): JUnit5ReplayDatabase = connection(readOnly = true)

  private def connection(readOnly: Boolean): JUnit5ReplayDatabase = {
    val columnFamilyDescriptors =
      ImmutableList.of(
        defaultColumnFamilyDescriptor,
        columnFamilyDescriptorForRecipes
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

    JUnit5ReplayDatabase(
      rocksDB,
      columnFamilyHandleForRecipes = columnFamilyHandles.get(1)
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
        ).getOrElse(runDatabaseDefault) + "-junit5"

        Path
          .of(directory)
          .resolve(file)
    }
}

case class JUnit5ReplayDatabase(
    rocksDb: RocksDB,
    columnFamilyHandleForRecipes: ColumnFamilyHandle
) extends AutoCloseable {
  def reset(): Unit = {
    dropColumnFamilyEntries(columnFamilyHandleForRecipes)
  }

  private def dropColumnFamilyEntries(
      columnFamilyHandle: ColumnFamilyHandle
  ): Unit =
    Using.resource(rocksDb.newIterator(columnFamilyHandle)) { iterator =>
      val firstKey: Array[Byte] = {
        iterator.seekToFirst()
        iterator.key
      }

      val onePastLastKey: Array[Byte] = {
        iterator.seekToLast()
        iterator.key() :+ 0
      }

      // NOTE: the range has an exclusive upper bound, hence the use of
      // `onePastLastKey`.
      rocksDb.deleteRange(
        columnFamilyHandle,
        firstKey,
        onePastLastKey
      )

    }

  def recordUniqueId(uniqueId: String, recipe: String): Unit = {
    rocksDb.put(
      columnFamilyHandleForRecipes,
      uniqueId.map(_.toByte).toArray,
      recipe.map(_.toByte).toArray
    )
  }

  def recipeFromUniqueId(uniqueId: String): Option[String] = Option(
    rocksDb
      .get(
        columnFamilyHandleForRecipes,
        uniqueId.map(_.toByte).toArray
      )
  ).map(_.map(_.toChar).mkString)

  def close(): Unit = {
    columnFamilyHandleForRecipes.close()
    rocksDb.close()
  }
}
