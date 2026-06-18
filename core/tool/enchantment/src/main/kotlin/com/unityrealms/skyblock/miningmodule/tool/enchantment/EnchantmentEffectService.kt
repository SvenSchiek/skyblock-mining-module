/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.EnchantmentEffectRegistry
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation.CacheToolEnchantmentStateRepository

import java.util.UUID

/**
 * Resolves configured passive effects through the enchantment effect registry.
 */
class EnchantmentEffectService(
  private val enchantmentRegistry: EnchantmentRegistry,
  private val effectRegistry: EnchantmentEffectRegistry,
  private val stateRepository: CacheToolEnchantmentStateRepository
) {

  fun damageMultiplier(toolIdentifier: UUID, context: ToolMiningContext? = null): Double {
    var multiplier = 1.0

    for ((definition, state, levelDefinition) in this.resolveActive(toolIdentifier)) {
      val effect = this.effectRegistry.get(definition.effectIdentifier) ?: continue
      multiplier *= effect.damageMultiplier(definition, levelDefinition, state, context)
    }

    return multiplier.coerceAtLeast(0.0)
  }

  fun fortuneBonus(toolIdentifier: UUID, context: ToolMiningContext? = null): Int {
    var bonus = 0

    for ((definition, state, levelDefinition) in this.resolveActive(toolIdentifier)) {
      val effect = this.effectRegistry.get(definition.effectIdentifier) ?: continue
      bonus += effect.fortuneBonus(definition, levelDefinition, state, context)
    }

    return bonus.coerceAtLeast(0)
  }

  fun executeBlockMinedEffects(context: ToolMiningContext) {
    if (!(context.allowAnySecondaryEnchantment)) {
      return
    }

    for ((definition, state, levelDefinition) in this.resolveActive(context.toolProfile.toolIdentifier)) {
      val effect = this.effectRegistry.get(definition.effectIdentifier) ?: continue
      val chainIdentifier = "${definition.identifier}:${effect.identifier}"

      if (!(context.toolMiningChainState.registerEffect(chainIdentifier))) {
        continue
      }

      effect.onBlockMined(definition, levelDefinition, state, context)
    }
  }

  fun activeSummary(toolIdentifier: UUID): List<String> = this.resolveActive(toolIdentifier).map { (definition, state, _) ->
    "${definition.displayName} ${state.level}/${definition.maximumLevel}"
  }

  private fun resolveActive(toolIdentifier: UUID): List<Triple<EnchantmentDefinition, ToolEnchantmentState, EnchantmentLevelDefinition>> {
    return this.stateRepository.getAllCached(toolIdentifier).mapNotNull { state ->
      if (!(state.unlocked) || state.level <= 0) return@mapNotNull null
      val definition = this.enchantmentRegistry.get(state.enchantmentIdentifier) ?: return@mapNotNull null
      if (!(definition.enabled)) return@mapNotNull null
      val levelDefinition = definition.levelDefinitionMap[state.level] ?: return@mapNotNull null
      val reachedBreakthroughList = (1..state.level).mapNotNull { level ->
        definition.levelDefinitionMap[level]?.breakthrough
      }
      val breakthroughConfiguration = reachedBreakthroughList.fold(emptyMap<String, Double>()) { configuration, breakthrough ->
        configuration + breakthrough.effectConfiguration
      }
      val effectiveLevelDefinition = levelDefinition.copy(
        effectConfiguration = levelDefinition.effectConfiguration + breakthroughConfiguration,
        breakthrough = reachedBreakthroughList.lastOrNull()
      )
      Triple(definition, state, effectiveLevelDefinition)
    }
  }
}
