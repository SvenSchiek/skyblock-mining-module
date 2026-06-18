/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatisticService
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.UnlockRequirementEvaluator
import com.unityrealms.skyblock.miningmodule.tool.UnlockRequirementProgress
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation.CacheToolEnchantmentStateRepository

/**
 * Automatically unlocks enchantments after all deterministic requirements are fulfilled.
 */
class EnchantmentUnlockService(
  private val enchantmentRegistry: EnchantmentRegistry,
  private val stateRepository: CacheToolEnchantmentStateRepository,
  private val statisticsService: ToolMiningStatisticService
) {

  fun evaluate(toolProfile: ToolProfile): List<EnchantmentDefinition> {
    val statistics = this.statisticsService.getCached(toolProfile.toolIdentifier) ?: return emptyList()
    val unlockedDefinitionList = mutableListOf<EnchantmentDefinition>()

    for (definition in this.enchantmentRegistry.all()) {
      if (!(definition.enabled)) continue
      val state = this.stateRepository.getCached(toolProfile.toolIdentifier, definition.identifier)
      if (state?.unlocked == true) continue
      if (!(UnlockRequirementEvaluator.matches(toolProfile, statistics, definition.unlockRequirementList))) continue

      this.stateRepository.save(
        ToolEnchantmentState(
          toolIdentifier = toolProfile.toolIdentifier,
          enchantmentIdentifier = definition.identifier,
          unlocked = true,
          level = state?.level ?: 0,
          investedEnchantmentPoints = state?.investedEnchantmentPoints ?: 0,
          investedFragments = state?.investedFragments ?: emptyMap(),
          updatedAt = System.currentTimeMillis()
        )
      )
      unlockedDefinitionList.add(definition)
    }

    return unlockedDefinitionList
  }

  fun progress(toolProfile: ToolProfile, definition: EnchantmentDefinition): List<UnlockRequirementProgress> {
    val statistics = this.statisticsService.getCached(toolProfile.toolIdentifier) ?: return emptyList()
    return definition.unlockRequirementList.flatMap { requirement ->
      UnlockRequirementEvaluator.progress(toolProfile, statistics, requirement)
    }
  }
}
