package com.sageserpent.americium.storage

import org.rocksdb.{ColumnFamilyHandle, RocksDB}

import scala.util.Using

/** Common functionality for RocksDB-backed databases in Americium.
  *
  * This trait provides the shared implementation for database operations that
  * are common across different database types (trials reproduction, JUnit5
  * replay, etc.).
  */
trait RocksDBDatabase extends AutoCloseable {

  /** Reset the database by clearing all column families.
    *
    * This removes all entries from all column families managed by this
    * database.
    */
  def reset(): Unit = {
    columnFamilyHandles.foreach(dropColumnFamilyEntries)
  }

  /** Drop all entries from a specific column family.
    *
    * @param columnFamilyHandle
    *   The column family to clear
    */
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

  /** Close the database and all its column family handles. */
  def close(): Unit = {
    columnFamilyHandles.foreach(_.close())
    rocksDb.close()
  }

  /** The underlying RocksDB instance */
  protected def rocksDb: RocksDB

  /** All column family handles that this database uses.
    *
    * Subclasses must provide this to enable operations like reset() and close()
    * to work across all column families.
    */
  protected def columnFamilyHandles: Seq[ColumnFamilyHandle]

  /** Store a string value in a column family.
    *
    * @param columnFamilyHandle
    *   The column family to write to
    * @param key
    *   The key (will be converted to bytes)
    * @param value
    *   The value (will be converted to bytes)
    */
  protected def putString(
      columnFamilyHandle: ColumnFamilyHandle,
      key: String,
      value: String
  ): Unit = {
    rocksDb.put(
      columnFamilyHandle,
      key.map(_.toByte).toArray,
      value.map(_.toByte).toArray
    )
  }

  /** Retrieve a string value from a column family.
    *
    * @param columnFamilyHandle
    *   The column family to read from
    * @param key
    *   The key (will be converted to bytes)
    * @return
    *   The value as a string, or None if not found
    */
  protected def getString(
      columnFamilyHandle: ColumnFamilyHandle,
      key: String
  ): Option[String] = {
    Option(
      rocksDb.get(
        columnFamilyHandle,
        key.map(_.toByte).toArray
      )
    ).map(_.map(_.toChar).mkString)
  }
}
