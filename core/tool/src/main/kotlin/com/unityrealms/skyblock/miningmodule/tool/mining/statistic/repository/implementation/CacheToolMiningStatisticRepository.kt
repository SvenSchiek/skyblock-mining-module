package com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatistic
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.ToolMiningStatisticRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Represents a write-behind cache for tool mining statistics.
 *
 * @property repository The underlying repository for loading and saving tool mining statistics.
 * @property logger The logger for logging cache operations and errors.
 */
class CacheToolMiningStatisticRepository(private val repository: ToolMiningStatisticRepository, private val logger: Logger) : AutoCloseable, ToolMiningStatisticRepository {

  companion object {
    private const val FLUSH_INTERVAL = 5L
  }

  private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "skyblock-mining-module:tool-statistic-cache").apply {
      this.isDaemon = true
    }
  }

  private val cache = ConcurrentHashMap<UUID, ToolMiningStatistic>()

  private val pendingToolIdentifierSet = ConcurrentHashMap.newKeySet<UUID>()

  init {
    this.scheduledExecutorService.scheduleAtFixedRate(this::flushAll, FLUSH_INTERVAL, FLUSH_INTERVAL, TimeUnit.MINUTES)
  }

  /**
   * Closes the cache tool mining statistic repository.
   */
  override fun close() {
    this.scheduledExecutorService.shutdown()

    this.flushAll()
  }


  /**
   * Saves the given tool mining statistic.
   *
   * @param toolMiningStatistic The tool mining statistic to save.
   */
  override fun save(toolMiningStatistic: ToolMiningStatistic) {
    this.cache[toolMiningStatistic.toolIdentifier] = toolMiningStatistic

    this.pendingToolIdentifierSet.add(toolMiningStatistic.toolIdentifier)
  }

  /**
   * Loads the tool mining statistic with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to load.
   *
   * @return The tool mining statistic with the specified identifier, or null if no statistic is found with that identifier.
   */
  override fun load(toolIdentifier: UUID): ToolMiningStatistic? {
    this.cache[toolIdentifier]?.let {
      return it
    }

    return this.repository.load(toolIdentifier)?.also {
      this.cache[toolIdentifier] = it
    }
  }


  /**
   * Deletes the tool mining statistic with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to delete.
   */
  override fun delete(toolIdentifier: UUID) {
    this.repository.delete(toolIdentifier)

    this.cache.remove(toolIdentifier)

    this.pendingToolIdentifierSet.remove(toolIdentifier)
  }


  /**
   * Warms the cache for the tool mining statistic with the specified identifier by loading it from the underlying repository and caching it.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to warm the cache for.
   *
   * @return The tool mining statistic with the specified identifier, or null if no statistic is found with that identifier in the underlying repository.
   */
  fun warm(toolIdentifier: UUID): ToolMiningStatistic? = this.load(toolIdentifier)


  /**
   * Gets the cached tool mining statistic with the specified identifier without loading it from the underlying repository if it is not already cached.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to get from the cache.
   *
   * @return The cached tool mining statistic with the specified identifier, or null if no statistic is found in the cache.
   */
  fun getCached(toolIdentifier: UUID): ToolMiningStatistic? = this.cache[toolIdentifier]

  /**
   * Replaces the cached tool mining statistic with the specified statistic, and marks it as pending to be flushed to the underlying repository.
   *
   * @param toolMiningStatistic The tool mining statistic to replace in the cache and mark as pending for flush.
   */
  fun replaceCached(toolMiningStatistic: ToolMiningStatistic) {
    this.cache[toolMiningStatistic.toolIdentifier] = toolMiningStatistic

    this.pendingToolIdentifierSet.remove(toolMiningStatistic.toolIdentifier)
  }


  /**
   * Flushes the cached tool mining statistic with the specified identifier to the underlying repository if it is pending for flush.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to flush to the underlying repository.
   */
  fun flush(toolIdentifier: UUID) {
    val toolMiningStatistic = this.cache[toolIdentifier] ?: return

    this.repository.save(toolMiningStatistic)

    this.pendingToolIdentifierSet.remove(toolIdentifier)
  }

  /**
   * Flushes all cached tool mining statistics that are pending for flush to the underlying repository.
   */
  fun flushAll() {
    for (toolIdentifier in ArrayList(this.pendingToolIdentifierSet)) {
      try {
        this.flush(toolIdentifier)
      } catch (exception: Exception) {
        this.logger.warning("Failed to flush mining tool statistics '$toolIdentifier': ${exception.message}")
      }
    }
  }


  /**
   * Unloads the cached tool mining statistic with the specified identifier from the cache.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to unload from the cache.
   */
  fun unload(toolIdentifier: UUID) {
    this.flush(toolIdentifier)

    this.cache.remove(toolIdentifier)
  }
}