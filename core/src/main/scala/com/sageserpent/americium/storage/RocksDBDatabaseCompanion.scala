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

/** Common functionality for RocksDB database companion objects.
  *
  * This trait provides the shared implementation for creating database
  * connections, managing RocksDB options, and computing database paths.
  *
  * @tparam DatabaseType
  *   The concrete database type (e.g., TrialsReproductionDatabase,
  *   JUnit5ReplayDatabase)
  */
trait RocksDBDatabaseCompanion[DatabaseType <: RocksDBDatabase] {

  /** Lazy evaluation of the database connection with automatic shutdown hook.
    */
  val evaluation: Eval[DatabaseType] =
    Eval.later {
      val result = connection(readOnly = false)
      Runtime.getRuntime.addShutdownHook(new Thread(() => result.close()))
      result
    }

  /** Common RocksDB options used across all database types. */
  protected val rocksDbOptions: DBOptions = new DBOptions()
    .optimizeForSmallDb()
    .setCreateIfMissing(true)
    .setCreateMissingColumnFamilies(true)

  /** Common column family options used across all database types. */
  protected val columnFamilyOptions: ColumnFamilyOptions =
    new ColumnFamilyOptions()
      .setCompressionType(CompressionType.LZ4_COMPRESSION)
      .setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION)

  /** The default column family descriptor (required by RocksDB). */
  protected val defaultColumnFamilyDescriptor: ColumnFamilyDescriptor =
    new ColumnFamilyDescriptor(
      RocksDB.DEFAULT_COLUMN_FAMILY,
      columnFamilyOptions
    )

  /** Create a read-only connection to the database.
    *
    * @return
    *   A read-only database instance
    */
  def readOnlyConnection(): DatabaseType = connection(readOnly = true)

  /** Create a database connection.
    *
    * @param readOnly
    *   Whether to open the database in read-only mode
    * @return
    *   The database instance
    */
  private def connection(readOnly: Boolean): DatabaseType = {
    // Combine default descriptor with specific descriptors
    val allDescriptors =
      defaultColumnFamilyDescriptor +: specificColumnFamilyDescriptors

    val columnFamilyDescriptors = ImmutableList.copyOf(allDescriptors.toArray)

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

    createDatabase(rocksDB, columnFamilyHandles)
  }

  /** Compute the path to the database directory.
    *
    * Uses Java system properties to determine the location, appending the
    * database-specific suffix.
    */
  protected def databasePath: Path =
    Option(System.getProperty(temporaryDirectoryJavaProperty)) match {
      case None =>
        throw new RuntimeException(
          s"No definition of Java property: `$temporaryDirectoryJavaProperty`"
        )

      case Some(directory) =>
        val file = Option(
          System.getProperty(runDatabaseJavaProperty)
        ).getOrElse(runDatabaseDefault) + databaseSuffix

        Path
          .of(directory)
          .resolve(file)
    }

  /** The suffix to append to the database name (e.g., "-trials", "-junit5").
    *
    * This distinguishes different database types in the filesystem.
    */
  protected def databaseSuffix: String

  /** The column family descriptors specific to this database type.
    *
    * Subclasses must provide the descriptors for their specific column
    * families. The default column family is already handled by the framework.
    */
  protected def specificColumnFamilyDescriptors: Seq[ColumnFamilyDescriptor]

  /** Create a database instance from a RocksDB connection and column family
    * handles.
    *
    * Subclasses implement this to construct their specific database type with
    * the appropriate column family handles extracted from the handles list.
    *
    * @param rocksDB
    *   The RocksDB instance
    * @param columnFamilyHandles
    *   All column family handles (index 0 is default, subsequent indices are
    *   specific families)
    * @return
    *   The concrete database instance
    */
  protected def createDatabase(
      rocksDB: RocksDB,
      columnFamilyHandles: JavaArrayList[ColumnFamilyHandle]
  ): DatabaseType
}
