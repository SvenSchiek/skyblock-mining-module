package com.unityrealms.skyblock.miningmodule.tool.mining.statistic

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.implementation.CacheToolMiningStatisticRepository

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a service for managing tool mining statistics.
 *
 * @property cacheToolMiningStatisticsRepository The repository for caching tool mining statistics.
 */
class ToolMiningStatisticService(private val cacheToolMiningStatisticsRepository: CacheToolMiningStatisticRepository) {

  private val lockMap = ConcurrentHashMap<UUID, Any>()


  /**
   * Gets the mining statistic for the tool with the specified identifier from the cache.
   *
   * @param toolIdentifier The unique identifier of the tool for which to get the mining statistic.
   *
   * @return The mining statistic for the tool with the specified identifier, or null if no statistic is found in the cache.
   */
  fun get(toolIdentifier: UUID): ToolMiningStatistic? = this.cacheToolMiningStatisticsRepository.load(toolIdentifier)

  /**
   * Gets the mining statistic for the tool with the specified identifier from the cache without loading it if it is not already cached.
   *
   * @param toolIdentifier The unique identifier of the tool for which to get the cached mining statistic.
   *
   * @return The cached mining statistic for the tool with the specified identifier, or null if no statistic is found in the cache.
   */
  fun getCached(toolIdentifier: UUID): ToolMiningStatistic? = this.cacheToolMiningStatisticsRepository.getCached(toolIdentifier)

  /**
   * Gets the mining statistic for the tool with the specified identifier from the cache, or creates a new statistic if no statistic is found in the cache.
   *
   * @param toolIdentifier The unique identifier of the tool for which to get or create the mining statistic.
   *
   * @return The mining statistic for the tool with the specified identifier, either loaded from the cache or newly created if no statistic is found in the cache.
   */
  fun getOrCreate(toolIdentifier: UUID): ToolMiningStatistic {
    return this.cacheToolMiningStatisticsRepository.load(toolIdentifier) ?: ToolMiningStatistic(toolIdentifier).also(this.cacheToolMiningStatisticsRepository::save)
  }


  /**
   * Records the mining of a block with the given category and ore category for the tool with the specified identifier in the cache.
   *
   * @param toolIdentifier The unique identifier of the tool for which to record the mined block.
   * @param blockCategory The category of the block that was mined.
   * @param oreCategory The category of the ore that was mined, or null if the mined block was not an ore.
   */
  fun recordCached(toolIdentifier: UUID, blockCategory: String, oreCategory: String?) {
    synchronized(this.lockFor(toolIdentifier)) {
      val toolMiningStatistic = this.cacheToolMiningStatisticsRepository.getCached(toolIdentifier) ?: return

      val normalizedBlockCategory = blockCategory.lowercase()

      val blockByCategoryMap = toolMiningStatistic.blockByCategoryMap.toMutableMap()

      blockByCategoryMap[normalizedBlockCategory] = (blockByCategoryMap[normalizedBlockCategory] ?: 0L) + 1L

      val oreByCategoryMap = toolMiningStatistic.oreByCategoryMap.toMutableMap()

      if (oreCategory != null) {
        val normalizedOreCategory = oreCategory.lowercase()

        oreByCategoryMap[normalizedOreCategory] = (oreByCategoryMap[normalizedOreCategory] ?: 0L) + 1L
      }

      this.cacheToolMiningStatisticsRepository.save(
        toolMiningStatistic.copy(
          totalMinedBlockCount = toolMiningStatistic.totalMinedBlockCount + 1L,
          totalMinedOreCount = if (oreCategory != null) {
            toolMiningStatistic.totalMinedOreCount + 1L
          } else {
            toolMiningStatistic.totalMinedOreCount
          },

          blockByCategoryMap = blockByCategoryMap,
          oreByCategoryMap = oreByCategoryMap,

          updatedAt = System.currentTimeMillis()
        )
      )
    }
  }

  /**
   * Records the discovery of a fragment with the given rarity for the tool with the specified identifier in the cache.
   *
   * @param toolIdentifier The unique identifier of the tool for which to record the found fragment.
   * @param fragmentRarity The rarity of the fragment that was found.
   */
  fun recordFragmentCached(toolIdentifier: UUID, fragmentRarity: FragmentRarity) {
    synchronized(this.lockFor(toolIdentifier)) {
      val toolMiningStatistic = this.cacheToolMiningStatisticsRepository.getCached(toolIdentifier) ?: return

      val fragmentMap = toolMiningStatistic.foundFragmentMap.toMutableMap()

      fragmentMap[fragmentRarity] = (fragmentMap[fragmentRarity] ?: 0L) + 1L

      this.cacheToolMiningStatisticsRepository.save(toolMiningStatistic.copy(
        foundFragmentMap = fragmentMap,

        updatedAt = System.currentTimeMillis())
      )
    }
  }


  /**
   * Flushes the cached mining statistics for the given tool identifier.
   *
   * @param toolIdentifier The unique identifier of the tool for which to flush the cached mining statistics.
   */
  fun flush(toolIdentifier: UUID) {
    this.cacheToolMiningStatisticsRepository.flush(toolIdentifier)
  }


  /**
   * Retrieves the lock object for the given identifier, creating a new one if it does not exist.
   *
   * @param identifier The unique identifier for which to retrieve the lock object.
   *
   * @return The lock object associated with the given identifier.
   */
  private fun lockFor(identifier: UUID): Any = this.lockMap.computeIfAbsent(identifier) {
    Any()
  }
}
