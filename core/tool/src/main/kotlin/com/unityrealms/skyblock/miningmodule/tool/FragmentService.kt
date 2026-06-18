/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatisticService
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.CacheFragmentStateRepository

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * Coordinates fragment balances, drops and independent soft-pity counters.
 */
class FragmentService(
  private val repository: CacheFragmentStateRepository,
  private val statisticsService: ToolMiningStatisticService,
  definitionList: List<FragmentDefinition>
) {

  @Volatile
  private var definitionMap = definitionList.associateBy(FragmentDefinition::rarity)

  fun update(definitionList: List<FragmentDefinition>) {
    this.definitionMap = definitionList.associateBy(FragmentDefinition::rarity)
  }

  fun getCachedStateList(toolIdentifier: UUID): List<FragmentState>? = this.repository.getAllCached(toolIdentifier)

  fun getAmount(toolIdentifier: UUID, rarity: FragmentRarity): Int = this.repository.getCached(toolIdentifier, rarity)?.amount ?: 0

  /** Performs one independent roll for every configured rarity. */
  fun rollCached(toolProfile: ToolProfile, miningContext: ToolMiningContext): List<FragmentDropResult> {
    if (!(miningContext.allowFragmentRolling)) {
      return emptyList()
    }

    val currentStateList = this.repository.getAllCached(toolProfile.toolIdentifier) ?: return emptyList()
    val currentStateMap = currentStateList.associateBy(FragmentState::rarity)
    val resultList = mutableListOf<FragmentDropResult>()

    for (rarity in FragmentRarity.entries) {
      val definition = this.definitionMap[rarity] ?: continue

      if (miningContext.activationSource !in definition.eligibleActivationSourceSet) {
        continue
      }

      if (miningContext.recursionDepth > 0 && !(definition.allowSecondaryBlocks)) {
        continue
      }

      val state = currentStateMap[rarity] ?: FragmentState(toolProfile.toolIdentifier, rarity)
      val effectiveChance = this.effectiveChance(toolProfile, state, definition)
      val hardPityTriggered = definition.hardPityThreshold != null && state.failedEligibleRolls + 1L >= definition.hardPityThreshold
      val dropped = hardPityTriggered || ThreadLocalRandom.current().nextDouble() < effectiveChance
      val updatedState = state.copy(
        amount = if (dropped) state.amount + 1 else state.amount,
        failedEligibleRolls = if (dropped) 0L else state.failedEligibleRolls + 1L,
        updatedAt = System.currentTimeMillis()
      )
      this.repository.save(updatedState)

      if (dropped) {
        this.statisticsService.recordFragmentCached(toolProfile.toolIdentifier, rarity)
      }

      resultList.add(FragmentDropResult(rarity, dropped, effectiveChance, hardPityTriggered, updatedState))
    }

    return resultList
  }

  private fun effectiveChance(
    toolProfile: ToolProfile,
    fragmentState: FragmentState,
    definition: FragmentDefinition
  ): Double {
    val adjustedSoftPityStart = (definition.softPityStart - toolProfile.prestige * definition.prestigeSoftPityReduction).coerceAtLeast(0L)
    val adjustedPityGrowth = definition.pityGrowth * (1.0 + toolProfile.prestige * definition.prestigePityGrowthMultiplier)
    val pityBonus = if (fragmentState.failedEligibleRolls >= adjustedSoftPityStart) {
      (fragmentState.failedEligibleRolls - adjustedSoftPityStart + 1L).toDouble() * adjustedPityGrowth
    } else {
      0.0
    }
    val prestigeMultiplier = 1.0 + toolProfile.prestige * definition.prestigeBaseChanceMultiplier

    return (
      definition.baseChance * prestigeMultiplier * toolProfile.fragmentChanceMultiplier + pityBonus
      ).coerceIn(0.0, 1.0)
  }
}
