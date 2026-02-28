package com.sageserpent.americium.storage

/** Common functionality for file-based storage implementations.
  *
  * Provides shared atomic write and TOCTOU-free read patterns for file-based
  * storage, ensuring thread-safe concurrent access.
  */
trait FileBasedStorage extends RecipeStorage {

  override def reset(): Unit = {
    // Remove entire storage directory
    // os-lib's remove.all handles "directory doesn't exist" gracefully
    os.remove.all(storageDirectory)
  }

  override def close(): Unit = {
    // File-based storage doesn't need explicit cleanup
    // OS will handle file handles
  }

  /** The directory where files are stored. Subclasses must provide this. */
  protected val storageDirectory: os.Path

  /** Atomically write content to a file using temp file + atomic rename.
    *
    * This ensures that readers never see partial writes, even under high
    * concurrency. Uses thread-unique temp filenames to avoid collisions between
    * concurrent writers.
    *
    * @param path
    *   The target file path to write to
    * @param content
    *   The content to write
    */
  protected def atomicWrite(path: os.Path, content: String): Unit = {
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

  /** Read a file, returning None if it doesn't exist.
    *
    * @param path
    *   The file path to read from
    * @return
    *   Some(contents) if file exists, None otherwise
    */
  protected def atomicReadOption(path: os.Path): Option[String] = {
    try {
      Some(atomicRead(path))
    } catch {
      case _: java.nio.file.NoSuchFileException => None
    }
  }

  /** Read a file without TOCTOU race conditions.
    *
    * Directly attempts to read the file without checking existence first, which
    * avoids time-of-check-to-time-of-use races.
    *
    * @param path
    *   The file path to read from
    * @return
    *   The file contents as a string
    * @throws java.nio.file.NoSuchFileException
    *   if the file doesn't exist
    */
  protected def atomicRead(path: os.Path): String = {
    // Read file directly - no exists() check to avoid TOCTOU race
    // If file doesn't exist, NoSuchFileException is thrown
    os.read(path)
  }
}
