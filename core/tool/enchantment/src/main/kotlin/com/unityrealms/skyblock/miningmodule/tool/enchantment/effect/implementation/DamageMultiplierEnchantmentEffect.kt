/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.implementation

import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentLevelDefinition
import com.unityrealms.skyblock.miningmodule.tool.enchantment.ToolEnchantmentState
import com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.EnchantmentEffect

/**
 * Adds a configured multiplier to custom mining damage.
 */
class DamageMultiplierEnchantmentEffect : EnchantmentEffect {

  override val identifier: String = "damage_multiplier"

  override fun damageMultiplier(
    definition: EnchantmentDefinition,
    levelDefinition: EnchantmentLevelDefinition,
    state: ToolEnchantmentState,
    context: ToolMiningContext?
  ): Double {
    val effectConfiguration = levelDefinition.effectConfiguration + levelDefinition.breakthrough?.effectConfiguration.orEmpty()
    val bonus = effectConfiguration["multiplier_bonus"]
      ?: effectConfiguration["value"]
      ?: 0.0
    return (1.0 + bonus).coerceAtLeast(0.0)
  }
}
