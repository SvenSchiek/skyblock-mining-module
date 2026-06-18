/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

import com.unityrealms.skyblock.miningmodule.tool.repository.ToolTransactionRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.CacheFragmentStateRepository
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation.CacheToolProfileRepository
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation.CacheToolEnchantmentStateRepository

import java.util.UUID

/**
 * Validates and processes atomic enchantment upgrades.
 */
class EnchantmentUpgradeService(
  private val enchantmentRegistry: EnchantmentRegistry,
  private val stateRepository: CacheToolEnchantmentStateRepository,
  private val profileRepository: CacheToolProfileRepository,
  private val fragmentStateRepository: CacheFragmentStateRepository,
  private val transactionRepository: ToolTransactionRepository
) {

  fun upgrade(toolIdentifier: UUID, enchantmentIdentifier: String): EnchantmentUpgradeResult {
    this.profileRepository.flush(toolIdentifier)
    this.fragmentStateRepository.flush(toolIdentifier)
    this.stateRepository.flush(toolIdentifier)
    val definition = this.enchantmentRegistry.get(enchantmentIdentifier)
      ?: throw IllegalArgumentException("Unknown enchantment '$enchantmentIdentifier'.")
    val state = this.stateRepository.getCached(toolIdentifier, definition.identifier)
      ?: throw IllegalStateException("The enchantment state is not loaded.")

    if (!(state.unlocked)) {
      throw IllegalArgumentException("The enchantment '${definition.identifier}' is not unlocked.")
    }

    if (state.level >= definition.maximumLevel) {
      throw IllegalStateException("The enchantment '${definition.identifier}' already reached its maximum level.")
    }

    val nextLevel = state.level + 1
    val levelDefinition = definition.levelDefinitionMap[nextLevel]
      ?: throw IllegalStateException("No upgrade definition exists for enchantment level $nextLevel.")
    val investedFragmentMap = state.investedFragments.toMutableMap()

    for ((rarity, amount) in levelDefinition.fragmentCostMap) {
      investedFragmentMap[rarity] = (investedFragmentMap[rarity] ?: 0) + amount
    }

    val now = System.currentTimeMillis()
    val transactionResult = this.transactionRepository.upgradeEnchantment(
      ToolTransactionRepository.EnchantmentUpgradeRequest(
        toolIdentifier = toolIdentifier,
        enchantmentIdentifier = definition.identifier,
        expectedLevel = state.level,
        nextLevel = nextLevel,
        enchantmentPointCost = levelDefinition.enchantmentPoints,
        fragmentCostMap = levelDefinition.fragmentCostMap,
        investedEnchantmentPoints = state.investedEnchantmentPoints + levelDefinition.enchantmentPoints,
        investedFragmentMap = investedFragmentMap,
        updatedAt = now
      )
    )
    val updatedState = state.copy(
      level = nextLevel,
      investedEnchantmentPoints = state.investedEnchantmentPoints + levelDefinition.enchantmentPoints,
      investedFragments = investedFragmentMap,
      updatedAt = now
    )
    this.profileRepository.replaceCached(transactionResult.toolProfile)
    transactionResult.fragmentStateList.forEach(this.fragmentStateRepository::replaceCached)
    this.stateRepository.replaceCached(updatedState)

    return EnchantmentUpgradeResult(
      state = updatedState,
      definition = definition,
      levelDefinition = levelDefinition,
      breakthrough = levelDefinition.breakthrough
    )
  }
}
