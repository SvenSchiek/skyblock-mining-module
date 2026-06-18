package com.unityrealms.skyblock.miningmodule.tool.mining.statistic

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity

import java.util.UUID

/**
 * Represents unlock-relevant mining statistics belonging to one tool.
 *
 * @property toolIdentifier The unique identifier of the tool this statistic belongs to.
 * @property totalMinedBlockCount The total number of blocks mined with this tool.
 * @property totalMinedOreCount The total number of ores mined with this tool.
 * @property blockByCategoryMap A map of block categories to the number of blocks mined in each category with this tool.
 * @property oreByCategoryMap A map of ore categories to the number of ores mined in each category with this tool.
 * @property foundFragmentMap A map of fragment rarities to the number of fragments found in each rarity with this tool.
 * @property updatedAt The timestamp of the last update to this statistic.
 */
data class ToolMiningStatistic(
  val toolIdentifier: UUID,

  val totalMinedBlockCount: Long = 0L,
  val totalMinedOreCount: Long = 0L,

  val blockByCategoryMap: Map<String, Long> = emptyMap(),
  val oreByCategoryMap: Map<String, Long> = emptyMap(),

  val foundFragmentMap: Map<FragmentRarity, Long> = emptyMap(),

  val updatedAt: Long = System.currentTimeMillis()
)
