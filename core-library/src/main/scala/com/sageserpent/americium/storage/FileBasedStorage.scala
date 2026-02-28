package com.sageserpent.americium.storage

/** Common functionality for file-based storage implementations.
  *
  * Provides shared atomic write and TOCTOU-free read patterns for file-based
  * storage, ensuring thread-safe concurrent access.
  */
trait FileBasedStorage {
  type Key

  /** The directory where files are stored. Subclasses must provide this. */
  protected val storageDirectory: os.Path

  def reset(): Unit = {
    // Remove entire storage directory
    // os-lib's remove.all handles "directory doesn't exist" gracefully
    os.remove.all(storageDirectory)
  }

  protected def filenameFor(recipeHash: Key): String

  /** Atomically write content to a file using temp file + atomic rename.
    *
    * This ensures that readers never see partial writes, even under high
    * concurrency. Uses thread-unique temp filenames to avoid collisions between
    * concurrent writers.
    *
    * @param key
    *   key identifying the file.
    * @param content
    *   The content to write
    */
  protected def atomicWrite(key: Key, content: String): Unit = {
    val path = storageDirectory / filenameFor(key)

    val threadSpecificNameToAvoidContention =
      s".${path.last}.${Thread.currentThread().getId}.tmp"
    val tempPath = path / os.up / threadSpecificNameToAvoidContention

    // Write to temp file with createFolders
    // This handles the case where directory doesn't exist
    os.write.over(tempPath, content, createFolders = true)

    // Atomic move with atomicMove=true for true OS-level atomicity
    // This prevents race conditions during concurrent moves
    os.move(tempPath, path, replaceExisting = true, atomicMove = true)
  }

  /** Read a file without TOCTOU race conditions.
    *
    * Directly attempts to read the file without checking existence first, which
    * avoids time-of-check-to-time-of-use races.
    *
    * @param key
    *   key identifying the file.
    * @return
    *   The file contents as a string
    * @throws java.nio.file.NoSuchFileException
    *   if the file doesn't exist
    */
  protected def atomicRead(key: Key): String = {
    val path = storageDirectory / filenameFor(key)

    // Read file directly - no exists() check to avoid TOCTOU race
    // If file doesn't exist, NoSuchFileException is thrown
    os.read(path)
  }
}
