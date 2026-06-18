package com.unityrealms.skyblock.miningmodule.tool.prestige

import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatisticService
import com.unityrealms.skyblock.miningmodule.tool.ToolPrestigeConfiguration
import com.unityrealms.skyblock.miningmodule.tool.ToolPrestigeResult
import com.unityrealms.skyblock.miningmodule.tool.configuration.ToolConfiguration
import com.unityrealms.skyblock.miningmodule.tool.repository.ToolTransactionRepository
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation.CacheToolProfileRepository
import java.util.UUID

/**
 * Validates and processes deterministic tool prestige transactions.
 */
@Deprecated("The tool prestige system is being redesigned and this class will be removed in a future update.")
class ToolPrestigeService(
  private val cacheToolProfileRepository: CacheToolProfileRepository,
  private val toolMiningStatisticService: ToolMiningStatisticService,
  private val toolTransactionRepository: ToolTransactionRepository,

  private var toolConfiguration: ToolConfiguration
) {

  /**
   * Updates the tool configuration used for prestige transactions.
   *
   * @param toolConfiguration The new tool configuration to use for prestige transactions.
   */
  fun update(toolConfiguration: ToolConfiguration) {
    this.toolConfiguration = toolConfiguration
  }


  /**
   * Checks if a tool is eligible for prestige.
   *
   * @param toolIdentifier The unique identifier of the tool to check for prestige eligibility.
   *
   * @return True if the tool meets all the requirements for prestige, false otherwise.
   */
  fun canPrestige(toolIdentifier: UUID): Boolean {
    val toolProfile = this.cacheToolProfileRepository.getCached(toolIdentifier) ?: return false

    val toolMiningStatistic = this.toolMiningStatisticService.getCached(toolIdentifier) ?: return false

    val nextPrestige = toolProfile.prestige + 1

    val requirement = this.toolConfiguration.toolPrestigeConfiguration.requirementMap[nextPrestige] ?: return false

    return toolProfile.prestige < this.toolConfiguration.toolPrestigeConfiguration.maximumPrestige &&
      (!(requirement.maximumPickaxeLevelRequired) || toolProfile.level >= this.toolConfiguration.maximumLevel) &&
      toolMiningStatistic.totalMinedBlockCount >= requirement.totalBlocksMined &&
      toolMiningStatistic.totalMinedOreCount >= requirement.totalOresMined
  }

  fun prestige(toolIdentifier: UUID): ToolPrestigeResult {
    this.cacheToolProfileRepository.flush(toolIdentifier)
    this.toolMiningStatisticService.flush(toolIdentifier)

    val previousToolProfile = this.cacheToolProfileRepository.getCached(toolIdentifier) ?: throw IllegalStateException("The tool mining profile is not loaded.")

    val toolMiningStatistic = this.toolMiningStatisticService.getCached(toolIdentifier) ?: throw IllegalStateException("The tool mining statistics are not loaded.")

    val nextPrestige = previousToolProfile.prestige + 1

    val requirement = this.toolConfiguration.toolPrestigeConfiguration.requirementMap[nextPrestige] ?: throw IllegalStateException("No prestige requirements are configured for prestige $nextPrestige.")
    val reward = this.toolConfiguration.toolPrestigeConfiguration.rewardMap[nextPrestige] ?: ToolPrestigeConfiguration.Reward()

    val toolProfile = this.toolTransactionRepository.prestige(
      toolProfile = previousToolProfile,

      toolMiningStatistic = toolMiningStatistic,
      maximumPickaxeLevel = this.toolConfiguration.maximumLevel,
      maximumPrestige = this.toolConfiguration.prestige.maximumPrestige,
      requirement = requirement,
      reward = reward
    )

    this.cacheToolProfileRepository.replaceCached(toolProfile)

    return ToolPrestigeResult(previousToolProfile, toolProfile, reward)
  }
}