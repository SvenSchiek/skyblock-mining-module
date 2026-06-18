package com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository

import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatistic

import java.util.UUID

/**
 * Represents a repository for tool mining statistics.
 */
interface ToolMiningStatisticRepository {

  /**
   * Saves the given tool mining statistic.
   *
   * @param toolMiningStatistic The tool mining statistic to save.
   */
  fun save(toolMiningStatistic: ToolMiningStatistic)

  /**
   * Loads the tool mining statistic with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to load.
   *
   * @return The tool mining statistic with the specified identifier, or null if no statistic is found with that identifier.
   */
  fun load(toolIdentifier: UUID): ToolMiningStatistic?


  /**
   * Deletes the tool mining statistic with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to delete.
   */
  fun delete(toolIdentifier: UUID)
}
