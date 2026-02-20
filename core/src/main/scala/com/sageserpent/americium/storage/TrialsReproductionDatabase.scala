package com.sageserpent.americium.storage

import com.sageserpent.americium.java.RecipeIsNotPresentException
import org.rocksdb.*

import _root_.java.util.ArrayList as JavaArrayList
import java.nio.file.Path

object TrialsReproductionDatabase
    extends RocksDBDatabaseCompanion[TrialsReproductionDatabase] {

  override protected val databasePath: Path = super.databasePath

  override protected def databaseSuffix: String = "-trials"

  override protected def specificColumnFamilyDescriptors
      : Seq[ColumnFamilyDescriptor] = Seq(
    new ColumnFamilyDescriptor(
      "RecipeHashKeyRecipeValue".getBytes(),
      columnFamilyOptions
    ),
    new ColumnFamilyDescriptor(
      "RecipeHashKeyGenerationMetadata".getBytes(),
      columnFamilyOptions
    )
  )

  override protected def createDatabase(
      rocksDB: RocksDB,
      columnFamilyHandles: JavaArrayList[ColumnFamilyHandle]
  ): TrialsReproductionDatabase = {
    TrialsReproductionDatabase(
      rocksDB,
      columnFamilyHandleForRecipes = columnFamilyHandles.get(1),
      columnFamilyHandleForStructuralOutlines = columnFamilyHandles.get(2)
    )
  }
}

case class TrialsReproductionDatabase(
    protected val rocksDb: RocksDB,
    columnFamilyHandleForRecipes: ColumnFamilyHandle,
    columnFamilyHandleForStructuralOutlines: ColumnFamilyHandle
) extends RocksDBDatabase {

  def recordRecipeHash(
      recipeHash: String,
      recipe: String,
      structureOutline: String
  ): Unit = {
    // Store the recipe.
    putString(columnFamilyHandleForRecipes, recipeHash, recipe)

    // Store the structural outline.
    putString(
      columnFamilyHandleForStructuralOutlines,
      recipeHash,
      structureOutline
    )
  }

  def recipeFromRecipeHash(recipeHash: String): String =
    getString(columnFamilyHandleForRecipes, recipeHash) match {
      case Some(value) => value
      case None        =>
        throw new RecipeIsNotPresentException(
          recipeHash,
          TrialsReproductionDatabase.databasePath
        )
    }

  def structureOutlineFromRecipeHash(
      recipeHash: String
  ): Option[String] = {
    getString(columnFamilyHandleForStructuralOutlines, recipeHash)
  }

  override protected def columnFamilyHandles: Seq[ColumnFamilyHandle] = Seq(
    columnFamilyHandleForRecipes,
    columnFamilyHandleForStructuralOutlines
  )
}
