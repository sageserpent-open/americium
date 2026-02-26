package com.sageserpent.americium.storage

/** Base trait for recipe storage implementations.
  *
  * Provides common interface for storing and retrieving test case recipes,
  * supporting multiple backend implementations (file-based, database, etc.).
  */
trait RecipeStorage extends AutoCloseable {

  /** Reset the storage by clearing all stored recipes.
    *
    * This removes all entries from storage.
    */
  def reset(): Unit

  /** Close the storage and release any resources. */
  def close(): Unit
}
