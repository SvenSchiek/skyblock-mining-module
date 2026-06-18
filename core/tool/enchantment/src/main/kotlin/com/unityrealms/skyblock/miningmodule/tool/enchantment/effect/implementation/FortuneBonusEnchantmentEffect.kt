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
 * Adds a configured integer fortune bonus to mining drops.
 */
class FortuneBonusEnchantmentEffect : EnchantmentEffect {

  override val identifier: String = "fortune_bonus"

  override fun fortuneBonus(
    definition: EnchantmentDefinition,
    levelDefinition: EnchantmentLevelDefinition,
    state: ToolEnchantmentState,
    context: ToolMiningContext?
  ): Int = (
    levelDefinition.effectConfiguration["fortune_bonus"]
      ?: levelDefinition.effectConfiguration["value"]
      ?: 0.0
    ).toInt().coerceAtLeast(0)
}
