package com.sageserpent.americium.junit5.storage

import com.sageserpent.americium.storage.{
  RocksDBDatabase,
  RocksDBDatabaseCompanion
}
import org.rocksdb.*

import _root_.java.util.ArrayList as JavaArrayList

object JUnit5ReplayDatabase
    extends RocksDBDatabaseCompanion[JUnit5ReplayDatabase] {

  override protected def databaseSuffix: String = "-junit5"

  override protected def specificColumnFamilyDescriptors
      : Seq[ColumnFamilyDescriptor] = Seq(
    new ColumnFamilyDescriptor(
      "TestCaseIdKeyRecipeValue".getBytes(),
      columnFamilyOptions
    )
  )

  override protected def createDatabase(
      rocksDB: RocksDB,
      columnFamilyHandles: JavaArrayList[ColumnFamilyHandle]
  ): JUnit5ReplayDatabase = {
    JUnit5ReplayDatabase(
      rocksDB,
      columnFamilyHandleForRecipes = columnFamilyHandles.get(1)
    )
  }
}

case class JUnit5ReplayDatabase(
    protected val rocksDb: RocksDB,
    columnFamilyHandleForRecipes: ColumnFamilyHandle
) extends RocksDBDatabase {

  def recordUniqueId(uniqueId: String, recipe: String): Unit = {
    putString(columnFamilyHandleForRecipes, uniqueId, recipe)
  }

  def recipeFromUniqueId(uniqueId: String): Option[String] = {
    getString(columnFamilyHandleForRecipes, uniqueId)
  }

  override protected def columnFamilyHandles: Seq[ColumnFamilyHandle] = Seq(
    columnFamilyHandleForRecipes
  )
}
