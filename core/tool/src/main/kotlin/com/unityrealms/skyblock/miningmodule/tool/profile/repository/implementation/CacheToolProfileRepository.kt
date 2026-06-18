package com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.ToolProfileRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Represents a write-behind cache for tool profiles.
 *
 * @property toolProfileRepository The underlying repository for loading and saving tool profiles.
 * @property logger The logger for logging cache operations and errors.
 */
class CacheToolProfileRepository(private val toolProfileRepository: ToolProfileRepository, private val logger: Logger) : AutoCloseable, ToolProfileRepository {

  companion object {
    private const val FLUSH_INTERVAL = 5L
  }

  private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "skyblock-mining-module:tool-profile-cache").apply {
      this.isDaemon = true
    }
  }


  private val cache = ConcurrentHashMap<UUID, ToolProfile>()

  private val pendingToolIdentifierSet = ConcurrentHashMap.newKeySet<UUID>()

  init {
    this.scheduledExecutorService.scheduleAtFixedRate(this::flushAll, FLUSH_INTERVAL, FLUSH_INTERVAL, TimeUnit.MINUTES)
  }

  /**
   * Closes the cache tool profile repository.
   */
  override fun close() {
    this.scheduledExecutorService.shutdown()

    this.flushAll()
  }


  /**
   * Saves the given tool profile.
   *
   * @param toolProfile The tool profile to save.
   */
  override fun save(toolProfile: ToolProfile) {
    this.cache[toolProfile.toolIdentifier] = toolProfile

    this.pendingToolIdentifierSet.add(toolProfile.toolIdentifier)
  }

  /**
   * Loads the tool profile with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool profile to load.
   *
   * @return The tool profile with the specified identifier, or null if no profile is found with that identifier.
   */
  override fun load(toolIdentifier: UUID): ToolProfile? {
    this.cache[toolIdentifier]?.let {
      return it
    }

    return this.toolProfileRepository.load(toolIdentifier)?.also {
      this.cache[toolIdentifier] = it
    }
  }


  /**
   * Deletes the tool profile with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool profile to delete.
   */
  override fun delete(toolIdentifier: UUID) {
    this.toolProfileRepository.delete(toolIdentifier)

    this.cache.remove(toolIdentifier)

    this.pendingToolIdentifierSet.remove(toolIdentifier)
  }


  /**
   * Warms the cache for the tool profile with the specified identifier by loading it into the cache if it is not already present.
   *
   * @param toolIdentifier The unique identifier of the tool profile to warm the cache for.
   *
   * @return The tool profile with the specified identifier.
   */
  fun warm(toolIdentifier: UUID): ToolProfile? = this.load(toolIdentifier)


  /**
   * Replaces the cached tool profile with the specified profile in the cache, and marks it as not pending for flush.
   *
   * @param toolProfile The tool profile to replace in the cache.
   */
  fun replaceCached(toolProfile: ToolProfile) {
    this.cache[toolProfile.toolIdentifier] = toolProfile

    this.pendingToolIdentifierSet.remove(toolProfile.toolIdentifier)
  }

  /**
   * Gets the cached tool profile with the specified identifier from the cache without loading it if it is not already cached.
   *
   * @param toolIdentifier The unique identifier of the tool profile to get from the cache.
   *
   * @return The cached tool profile with the specified identifier, or null if no profile is found in the cache.
   */
  fun getCached(toolIdentifier: UUID): ToolProfile? = this.cache[toolIdentifier]


  /**
   * Flushes the cached tool profile with the specified identifier to the underlying repository.
   *
   * @param toolIdentifier The unique identifier of the tool profile to flush.
   */
  fun flush(toolIdentifier: UUID) {
    val toolProfile = this.cache[toolIdentifier] ?: return

    this.toolProfileRepository.save(toolProfile)

    this.pendingToolIdentifierSet.remove(toolIdentifier)
  }

  /**
   * Flushes all pending tool profiles in the cache to the underlying repository.
   */
  fun flushAll() {
    for (toolIdentifier in ArrayList(this.pendingToolIdentifierSet)) {
      try {
        this.flush(toolIdentifier)
      } catch (exception: Exception) {
        this.logger.warning("Failed to flush tool profile '$toolIdentifier': ${exception.message}")
      }
    }
  }


  /**
   * Unloads the cached tool profile with the specified identifier from the cache.
   *
   * @param toolIdentifier The unique identifier of the tool profile to unload from the cache.
   */
  fun unload(toolIdentifier: UUID) {
    this.flush(toolIdentifier)

    this.cache.remove(toolIdentifier)
  }
}
