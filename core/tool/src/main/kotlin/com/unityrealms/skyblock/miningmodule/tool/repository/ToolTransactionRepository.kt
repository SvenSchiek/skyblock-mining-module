/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.repository

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.FragmentState
import com.unityrealms.skyblock.miningmodule.tool.ToolAbilityState
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatistic
import com.unityrealms.skyblock.miningmodule.tool.ToolPrestigeConfiguration
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile

import java.util.UUID

/**
 * Represents atomic database transactions for important tool operations.
 */
interface ToolTransactionRepository {

  data class EnchantmentUpgradeRequest(
    val toolIdentifier: UUID,
    val enchantmentIdentifier: String,
    val expectedLevel: Int,
    val nextLevel: Int,
    val enchantmentPointCost: Int,
    val fragmentCostMap: Map<FragmentRarity, Int>,
    val investedEnchantmentPoints: Int,
    val investedFragmentMap: Map<FragmentRarity, Int>,
    val updatedAt: Long
  )

  data class EnchantmentUpgradeResult(
    val toolProfile: ToolProfile,
    val fragmentStateList: List<FragmentState>
  )

  data class AbilityUpgradeRequest(
    val toolIdentifier: UUID,
    val abilityIdentifier: String,
    val expectedLevel: Int,
    val nextLevel: Int,
    val enchantmentPointCost: Int,
    val fragmentCostMap: Map<FragmentRarity, Int>,
    val updatedAt: Long
  )

  data class AbilityUpgradeResult(
    val toolProfile: ToolProfile,
    val abilityState: ToolAbilityState,
    val fragmentStateList: List<FragmentState>
  )

  fun upgradeEnchantment(request: EnchantmentUpgradeRequest): EnchantmentUpgradeResult

  fun upgradeAbility(request: AbilityUpgradeRequest): AbilityUpgradeResult

  fun prestige(
    toolProfile: ToolProfile,
    toolMiningStatistic: ToolMiningStatistic,
    maximumPickaxeLevel: Int,
    maximumPrestige: Int,
    requirement: ToolPrestigeConfiguration.Requirement,
    reward: ToolPrestigeConfiguration.Reward
  ): ToolProfile

  fun selectAbility(toolIdentifier: UUID, abilityIdentifier: String): ToolProfile

  fun activateAbility(toolIdentifier: UUID, abilityIdentifier: String, cooldownEndsAt: Long): ToolAbilityState
}
